package yakxin.columbina.utils.utilsView;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.HighlightHelper;

import yakxin.columbina.utils.UtilsData;

/**
 * 地图高亮工具
 * <p>封装 JOSM 的 {@link HighlightHelper} 和选中逻辑，提供统一的高亮/清除接口。
 * 构造时自动保存当前选中要素和视图状态，清除时自动还原。
 */
public class Highlighter {
    private final HighlightHelper highlightHelper = new HighlightHelper();
    private final List<OsmPrimitive> savedSelection;
    private final EastNorth savedMapCenter;
    private final double savedMapScale;

    public Highlighter() {
        DataSet ds = UtilsData.getEditDataSet();
        savedSelection = ds != null ? new ArrayList<>(ds.getSelected()) : new ArrayList<>();
        savedMapCenter = MainApplication.getMap().mapView.getCenter();
        savedMapScale = MainApplication.getMap().mapView.getScale();
    }

    /**
     * 高亮指定的路径和节点
     * <p>选中并高亮指定路径和节点，自动缩放到选中范围。
     * @param wayIds 要高亮的路径ID列表（可为空）
     * @param nodeIds 要高亮的节点ID列表（可为空）
     */
    public void highlight(List<Long> wayIds, List<Long> nodeIds) {
        DataSet ds = UtilsData.getEditDataSet();
        if (ds == null) return;

        List<Node> nodeToHighlight = new ArrayList<>();
        List<Way> wayToHighlight = new ArrayList<>();
        List<OsmPrimitive> featureToSelect = new ArrayList<>();

        if (wayIds != null) {
            for (long wayId : wayIds) {
                Way way = UtilsData.findWayById(wayId);
                if (way != null) wayToHighlight.add(way);
            }
        }
        if (nodeIds != null) {
            for (long nodeId : nodeIds) {
                Node node = UtilsData.findNodeById(nodeId);
                if (node != null && !nodeToHighlight.contains(node)) nodeToHighlight.add(node);
            }
        }

        featureToSelect.addAll(wayToHighlight);
        featureToSelect.addAll(nodeToHighlight);
        ds.setSelected(featureToSelect);

        if (highlightHelper.highlightOnly(nodeToHighlight)) {
            MainApplication.getMap().repaint();
        }

        if (!featureToSelect.isEmpty()) {
            AutoScaleAction.autoScale(AutoScaleAction.AutoScaleMode.SELECTION);
        }
    }

    /**
     * 清除高亮，还原构造时保存的选中要素和视图位置
     */
    public void clear() {
        highlightHelper.clear();
        HighlightHelper.clearAllHighlighted();
        DataSet ds = UtilsData.getEditDataSet();
        if (ds != null) {
            ds.setSelected(savedSelection);
        }
        MainApplication.getMap().mapView.zoomTo(savedMapCenter, savedMapScale);
        if (MainApplication.getMap() != null) {
            MainApplication.getMap().repaint();
        }
    }
}