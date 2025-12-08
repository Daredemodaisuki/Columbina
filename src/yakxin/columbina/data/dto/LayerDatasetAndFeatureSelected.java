package yakxin.columbina.data.dto;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import java.util.List;

/// 数据类
public final class LayerDatasetAndFeatureSelected<SelectionType extends OsmPrimitive> {
    public final OsmDataLayer layer;
    public final DataSet dataset;
    public final List<SelectionType> selection;

    public LayerDatasetAndFeatureSelected(OsmDataLayer layer, DataSet dataset, List<SelectionType> selection) {
        this.layer = layer;
        this.dataset = dataset;
        this.selection = selection;
    }
}


