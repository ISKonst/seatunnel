package org.apache.seatunnel.transform.dynamiccompile;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.connector.TableTransform;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableTransformFactory;
import org.apache.seatunnel.api.table.factory.TableTransformFactoryContext;
import org.apache.seatunnel.transform.common.TransformCommonOptions;

@AutoService(Factory.class)
public class DynamicCompilesTransformFactory implements TableTransformFactory {

    @Override
    public String factoryIdentifier() {
        return DynamicCompilesTransform.PLUGIN_NAME;
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
                .optional(
                        DynamicCompileTransformConfig.COMPILE_LANGUAGE,
                        DynamicCompileTransformConfig.COMPILE_PATTERN)
                .conditional(
                        DynamicCompileTransformConfig.COMPILE_PATTERN,
                        CompilePattern.SOURCE_CODE,
                        DynamicCompileTransformConfig.SOURCE_CODE)
                .conditional(
                        DynamicCompileTransformConfig.COMPILE_PATTERN,
                        CompilePattern.ABSOLUTE_PATH,
                        DynamicCompileTransformConfig.ABSOLUTE_PATH)
                .optional(TransformCommonOptions.MULTI_TABLES)
                .optional(TransformCommonOptions.TABLE_MATCH_REGEX)
                .build();
    }

    @Override
    public TableTransform createTransform(TableTransformFactoryContext context) {
        return () -> new DynamicCompilesMultiCatalogFlatMapTransform(context.getCatalogTables(), context.getOptions());
    }
}
