package yakxin.columbina.data.dto;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import java.util.List;

/// 数据类
public final class LayerDatasetAndWaySelected {
    public final OsmDataLayer layer;
    public final DataSet dataset;
    public final List<Way> selectedWays;

    public LayerDatasetAndWaySelected(OsmDataLayer layer, DataSet dataset, List<Way> selectedWays) {
        this.layer = layer;
        this.dataset = dataset;
        this.selectedWays = selectedWays;
    }
}


