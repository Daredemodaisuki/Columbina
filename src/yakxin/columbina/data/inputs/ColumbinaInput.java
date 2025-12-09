package yakxin.columbina.data.inputs;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaException;

import java.util.ArrayList;
import java.util.List;

public class ColumbinaInput {
    private OsmDataLayer layer = null;
    private DataSet dataSet =null;
    private List<Node> nodes = new ArrayList<>();
    private List<Way> ways = new ArrayList<>();

    public ColumbinaInput() {getLayerDatasetAndFeaturesSelected();}
    public ColumbinaInput(Node node) {this.nodes.add(node);}
    public ColumbinaInput(Way way) {this.ways.add(way);}
    public ColumbinaInput(Node node, Way way) {this.nodes.add(node); this.ways.add(way);}
    public ColumbinaInput(List<Node> nodes, List<Way> ways) {this.nodes = nodes; this.ways = ways;}
    public <Type extends OsmPrimitive> ColumbinaInput(List<Type> singleTypeList, Class<Type> type) {
        if (singleTypeList.isEmpty()) return;
        if (type == Node.class) {
            for (Type item : singleTypeList) {if (item instanceof Node) this.nodes.add((Node) item);}
        }
        if (type == Way.class) {
            for (Type item : singleTypeList) {if (item instanceof Way) this.ways.add((Way) item);}
        }
    }

    public OsmDataLayer getLayer() {
        return layer;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Way> getWays() {
        return ways;
    }

    private LayerDataSet getLayerDataSet() {
        OsmDataLayer newLayer = MainApplication.getLayerManager().getEditLayer();  // 当前的编辑图层
        if (newLayer == null) throw new ColumbinaException(I18n.tr("Current layer is not available."));
        DataSet newDataset = MainApplication.getLayerManager().getEditDataSet();  // 当前的编辑数据库
        if (newDataset == null) throw new ColumbinaException(I18n.tr("Current dataset is not available."));
        return new LayerDataSet(newLayer, newDataset);
    }

    /**
     * 获取选中的图层、数据库和路径，用于初始化或刷新类实体的储存内容
     * <p>如果图层、数据库有问题，将抛出ColumbinaException异常
     */
    public void getLayerDatasetAndFeaturesSelected() {
        LayerDataSet layerDataSet = getLayerDataSet();
        // 按照类型分类放置
        List<Node> newNodes = new ArrayList<>();
        List<Way> newWays = new ArrayList<>();
        for (OsmPrimitive p : layerDataSet.layer.data.getSelected()) {
            if (p instanceof Node) newNodes.add((Node) p);
            else if (p instanceof Way) newWays.add((Way) p);
            else continue;
        }

        // 上述均没有问题后再统一刷新实体储存之内容
        this.layer = layerDataSet.layer; this.dataSet = layerDataSet.dataSet;
        this.nodes = newNodes; this.ways = newWays;
    }

    public int getInputNum() {return ways.size() + nodes.size();}
    public <Type extends OsmPrimitive> int getInputNum(Class<Type> type) {
        if (type == Node.class && nodes != null) return nodes.size();
        else if (type == Way.class && ways != null) return ways.size();
        else return 0;
    }

    public <Type extends OsmPrimitive> Type getItem(Class<Type> type, int index) {
        if (type == Node.class && index >= 0 && index < nodes.size()) return type.cast(nodes.get(index));
        else if (type == Way.class && index >=0 && index < ways.size()) return type.cast(ways.get(index));
        else return null;
    }

    @SuppressWarnings("unchecked")
    public <Type extends OsmPrimitive> List<Type> getList(Class<Type> type) {
        if (type == Node.class && nodes != null) return (List<Type>) nodes;
        else if (type == Way.class && ways != null) return (List<Type>) ways;
        else return null;
    }

    public boolean isValid() {return (nodes != null && ways != null);}

    public boolean isEmpty() {return (nodes.isEmpty() && ways.isEmpty());}
    public <Type extends OsmPrimitive> boolean isEmpty(Class<Type> type) {
        if (type == Node.class) return nodes.isEmpty();
        else if (type == Way.class) return ways.isEmpty();
        else return true;
    }

    public String getDescription() {
        if (nodes.isEmpty() && ways.isEmpty()) return ColumbinaInputEnum.EMPTY.name();
        else if (!nodes.isEmpty() && ways.isEmpty()) return ColumbinaInputEnum.NODE.name();
        else if (nodes.isEmpty()) return ColumbinaInputEnum.WAY.name();
        else return ColumbinaInputEnum.NODE_WAY.name();
    }

    private static class LayerDataSet {
        public final OsmDataLayer layer;
        public final DataSet dataSet;

        public LayerDataSet(OsmDataLayer layer, DataSet dataSet) {
            this.layer = layer;
            this.dataSet = dataSet;
        }
    }
}
