package org.apache.seatunnel.transform.dynamiccompile;

import lombok.NonNull;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.api.table.catalog.*;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowAccessor;
import org.apache.seatunnel.common.utils.FileUtils;
import org.apache.seatunnel.common.utils.ReflectionUtils;
import org.apache.seatunnel.transform.common.AbstractCatalogSupportFlatMapTransform;
import org.apache.seatunnel.transform.dynamiccompile.parse.AbstractParse;
import org.apache.seatunnel.transform.dynamiccompile.parse.GroovyClassParse;
import org.apache.seatunnel.transform.dynamiccompile.parse.JavaClassParse;
import org.apache.seatunnel.transform.exception.TransformException;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.seatunnel.transform.dynamiccompile.CompileTransformErrorCode.COMPILE_TRANSFORM_ERROR_CODE;

public class DynamicCompilesTransform extends AbstractCatalogSupportFlatMapTransform {

    public static final String PLUGIN_NAME = "DynamicCompiles";

    public static final String getInlineOutputColumns = "getInlineOutputColumns";

    public static final String getInlineOutputFieldValues = "getInlineOutputFieldValues";

    private final String sourceCode;

    private final boolean compatibilityMode;

    private AbstractParse dynamicCompileParse;

    public DynamicCompilesTransform(@NonNull CatalogTable inputCatalogTable, @NonNull ReadonlyConfig readonlyConfig) {
        super(inputCatalogTable);
        CompileLanguage compileLanguage =
                readonlyConfig.get(DynamicCompileTransformConfig.COMPILE_LANGUAGE);
        // todo other compile
        if (CompileLanguage.GROOVY.equals(compileLanguage)) {
            dynamicCompileParse = new GroovyClassParse();
        } else if (CompileLanguage.JAVA.equals(compileLanguage)) {
            dynamicCompileParse = new JavaClassParse();
        }
        CompilePattern compilePattern = readonlyConfig.get(DynamicCompileTransformConfig.COMPILE_PATTERN);
        if (CompilePattern.SOURCE_CODE.equals(compilePattern)) {
            sourceCode = readonlyConfig.get(DynamicCompileTransformConfig.SOURCE_CODE);
        } else {
            // NPE will never happen because it is required in the ABSOLUTE_PATH mode
            sourceCode =
                    FileUtils.readFileToStr(
                            Paths.get(
                                    readonlyConfig.get(
                                            DynamicCompileTransformConfig.ABSOLUTE_PATH)));
        }
        compatibilityMode = sourceCode.contains(SeaTunnelRowAccessor.class.getName());
    }

    @Override
    protected List<SeaTunnelRow> transformRow(SeaTunnelRow inputRow) {
        return getOutputFieldValues(new SeaTunnelRowAccessor(inputRow));
    }

    @Override
    protected TableSchema transformTableSchema() {
        TableSchema.Builder builder = TableSchema.builder();
        if (inputCatalogTable.getTableSchema().getPrimaryKey() != null) {
            builder.primaryKey(inputCatalogTable.getTableSchema().getPrimaryKey().copy());
        }
        builder.constraintKey(inputCatalogTable.getTableSchema().getConstraintKeys().stream()
                .map(ConstraintKey::copy)
                .collect(Collectors.toList()));
        return builder.columns(Arrays.stream(getOutputColumns()).collect(Collectors.toList())).build();
    }

    @Override
    protected TableIdentifier transformTableIdentifier() {
        return inputCatalogTable.getTableId().copy();
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    private Column[] getOutputColumns() {
        Object result;
        try {
            result =
                    ReflectionUtils.invoke(
                            getCompileLanguageInstance(),
                            getInlineOutputColumns,
                            inputCatalogTable);

        } catch (Exception e) {
            throw new TransformException(COMPILE_TRANSFORM_ERROR_CODE, e.getMessage());
        }
        return (Column[]) result;
    }

    private List<SeaTunnelRow> getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        Object result;
        try {
            result =
                    ReflectionUtils.invoke(
                            getCompileLanguageInstance(),
                            getInlineOutputFieldValues,
                            getCompatibilityAccessor(inputRow));
        } catch (Exception e) {
            throw new TransformException(COMPILE_TRANSFORM_ERROR_CODE, e.getMessage());
        }
        return (List<SeaTunnelRow>) result;
    }

    private Object getCompileLanguageInstance()
            throws InstantiationException, IllegalAccessException {
        Class<?> compileClass = dynamicCompileParse.parseClassSourceCode(sourceCode);
        return compileClass.newInstance();
    }

    private Object getCompatibilityAccessor(SeaTunnelRowAccessor inputRow) {
        if (compatibilityMode) {
            Optional<Object> field = ReflectionUtils.getField(inputRow, "row");
            SeaTunnelRow row = (SeaTunnelRow) field.get();
            return new SeaTunnelRowAccessor(row);
        }
        return inputRow;
    }
}
