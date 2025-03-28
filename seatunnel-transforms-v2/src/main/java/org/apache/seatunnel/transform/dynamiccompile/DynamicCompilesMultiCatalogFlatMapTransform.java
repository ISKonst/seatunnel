package org.apache.seatunnel.transform.dynamiccompile;

import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.transform.SeaTunnelTransform;
import org.apache.seatunnel.transform.common.AbstractMultiCatalogFlatMapTransform;

import java.util.List;

public class DynamicCompilesMultiCatalogFlatMapTransform extends AbstractMultiCatalogFlatMapTransform {

    public DynamicCompilesMultiCatalogFlatMapTransform(List<CatalogTable> inputCatalogTables, ReadonlyConfig config) {
        super(inputCatalogTables, config);
    }

    @Override
    protected SeaTunnelTransform<SeaTunnelRow> buildTransform(CatalogTable inputCatalogTable, ReadonlyConfig config) {
        return new DynamicCompilesTransform(inputCatalogTable, config);
    }

    @Override
    public String getPluginName() {
        return DynamicCompilesTransform.PLUGIN_NAME;
    }
}
