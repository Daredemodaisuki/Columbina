package yakxin.columbina.features.fillet.advanced;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.features.fillet.FilletParams;
import yakxin.columbina.utils.UtilsMath;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdvFilletDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    private JTable table;
    private DefaultTableModel tableModel;
    private final JComboBox<WayComboItem> wayComboBox;
    private final JFormattedTextField batchRadiusInput;

    // 拐角数据
    private final ColumbinaInput input;
    private final List<OsmPrimitive> savedSelection;  // 窗口打开时的选中要素，用于关闭时还原
    private final HighlightHelper highlightHelper = new HighlightHelper();
    private final List<CornerInfo> allCorners;
    private final double[] editedRadii;  // 用于临时记录半径修改值
    private List<Integer> cornerIdxDisplaying = new ArrayList<>();  // 当前正在列表中展示的每个拐角在allCorners/editedRadii的索引
    private final Map<Long, List<Integer>> cornerIdxMap;  // 每条路径每个拐角在allCorners/editedRadii的索引，用于快速筛选{Way1: [1,2,3], Way2: [4,5,6,7], ...}

    public AdvFilletDialog(FilletParams savedParams, ColumbinaInput input) {
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);
        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);

        this.input = input;
        // 记录窗口打开时的选中要素，用于关闭时还原
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        savedSelection = ds != null ? new ArrayList<>(ds.getSelected()) : new ArrayList<>();
        // 计算所有拐角信息
        allCorners = computeCorners(input, savedParams);
        // 初始化用户编辑半径数组（默认值 = 推荐最大半径）
        editedRadii = new double[allCorners.size()];
        for (int i = 0; i < allCorners.size(); i++) {
            editedRadii[i] = allCorners.get(i).preferredMaxRadius;
        }
        // 按路径ID分组（记录allCorners/editedRadii索引）
        cornerIdxMap = groupCornerIdxByWayId(allCorners);
        
        wayComboBox = buildWayComboBox();  // 构建下拉选择框
        JScrollPane tableScrollPane = makeAndGetTable();  // 构建表格（初始显示全部）

        // 窗体
        JPanel panel = new JPanel(new GridBagLayout());
        UtilsUI.addHeader(panel,
                UtilsUI.ADVANCED_ITALIC + "<br>"
                        + I18n.tr("Round Corners"),
                "RoundCorners"
        );

        UtilsUI.addSection(panel, I18n.tr("Corner Information"));

        // 下拉选择框
        UtilsUI.addCombo(panel, wayComboBox, I18n.tr("Filter by way:"));

        // 列表
        panel.add(tableScrollPane, GBC.eol().fill(GridBagConstraints.HORIZONTAL));

        // 批量设置半径
        UtilsUI.addSpace(panel, 3);
        batchRadiusInput = UtilsUI.addInput(
                panel, I18n.tr("Batch set radius (m): "),
                String.valueOf(savedParams.surfaceRadius), GBC.std()
        );
        UtilsUI.addButton(
                panel, I18n.tr("Apply to selected"),
                (ActionEvent e) -> applyBatchRadius()
        );

        if (allCorners.isEmpty()) {
            UtilsUI.addSpace(panel, 5);
            JLabel hint = new JLabel(I18n.tr("No corners found in the selected ways."));
            hint.setForeground(Color.GRAY);
            panel.add(hint, GBC.eol());
        }

        contentInsets = new Insets(5, 15, 5, 15);
        setContent(panel);

        // 窗口关闭时清除高亮
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                clearHighlight();
            }
        });

        setupDialog();
        showDialog();
    }

    /**
     * 构建路径筛选下拉选择框
     * @return 下拉选择框
     */
    private JComboBox<WayComboItem> buildWayComboBox() {
        JComboBox<WayComboItem> comboBox = new JComboBox<>();
        // 添加「全部」选项
        comboBox.addItem(new WayComboItem(true, 0, I18n.tr("All")));
        // 添加各路径选项
        // int wayIndex = 1;
        for (Map.Entry<Long, List<Integer>> entry : cornerIdxMap.entrySet()) {
            long wayId = entry.getKey();
            int cornerCount = entry.getValue().size();
            String label = I18n.tr("Way{0} ({1} corners)", wayId, cornerCount);
            comboBox.addItem(new WayComboItem(false, wayId, label));
            // wayIndex++;
        }
        // 选择变更时刷新表格并更新高亮
        comboBox.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                refreshTable();
                updateHighlight();
            }
        });
        return comboBox;
    }

    /**
     * 在刷新表格前，将当前表格中用户编辑的半径保存到editedRadii
     */
    private void saveCurrentTableEdits() {
        for (int tableRow = 0; tableRow < cornerIdxDisplaying.size(); tableRow++) {
            // 获取刷新前每一行拐角在allCorners/editedRadii的索引
            int cornerIdx = cornerIdxDisplaying.get(tableRow);
            // 更新储存值
            Object value = tableModel.getValueAt(tableRow, 5);  // 第5列为「半径」列
            if (value instanceof Number) {
                editedRadii[cornerIdx] = ((Number) value).doubleValue();
            }
        }
    }

    /**
     * 将批量输入框中的半径值应用到表格中所有选中行
     */
    private void applyBatchRadius() {
        // 读取输入值
        double radius;
        try {
            radius = NumberFormat.getInstance(Locale.US).parse(batchRadiusInput.getText()).doubleValue();
        } catch (ParseException e) {
            return;  // 输入无效时不做操作
        }

        // 退出单元格编辑模式
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        // 获取选中行
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) return;  // 没有选中行时不做操作

        // 设置选中行的半径
        for (int row : selectedRows) {
            tableModel.setValueAt(radius, row, 5);
        }
        // 同步到editedRadii
        saveCurrentTableEdits();
    }

    /**
     * 根据当前下拉选择框和列表选中行更新地图上的高亮
     * <p>高亮逻辑：
     * <ul>
     *     <li>下拉选择框选中某条路径时，高亮该路径及其所有节点</li>
     *     <li>列表中有选中行时，额外高亮选中行对应的拐角节点</li>
     * </ul>
     * <p>路径仅选中，节点需要进一步高亮；
     * <p>节点需要被选中才能进一步高亮，因此临时选中相关节点，
     * 并在清除高亮/关闭窗口时还原回窗口打开时的选中要素。
     */
    private void updateHighlight() {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null) return;

        List<Node> nodeToHighlight = new ArrayList<>();
        List<Way> wayToHighlight = new ArrayList<>();
        List<OsmPrimitive> featureToSelect = new ArrayList<>();

        // 根据下拉选择框确定要高亮的路径节点
        WayComboItem selected = (WayComboItem) wayComboBox.getSelectedItem();
        if (selected != null) {
            if (!selected.isAll) {
                Way way = findWayById(selected.wayId);
                if (way != null) wayToHighlight.add(way);
            } else if (table.getSelectedRows().length == 0) {
                clearHighlight();  // 下拉框选中全部，且列表无选中则还原至清除高亮状态
                return;
            }
        }
        
        // 根据列表选中行确定要高亮的拐角节点
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            if (row < cornerIdxDisplaying.size()) {
                int cornerIdx = cornerIdxDisplaying.get(row);
                Node node = findNodeById(allCorners.get(cornerIdx).nodeId);
                if (node != null && !nodeToHighlight.contains(node)) nodeToHighlight.add(node);
            }
        }

        // 临时选中要高亮的要素（节点需被选中才能在地图上显示高亮）
        featureToSelect.addAll(wayToHighlight);
        featureToSelect.addAll(nodeToHighlight);
        ds.setSelected(featureToSelect);
        // 同时设置高亮标记（为路径提供额外的高亮视觉效果）
        if (highlightHelper.highlightOnly(nodeToHighlight)) {
            MainApplication.getMap().repaint();
        }
    }

    /**
     * 根据唯一ID在当前数据集中查找路径
     * @param wayId 路径唯一ID
     * @return 路径对象，未找到则返回null
     */
    private Way findWayById(long wayId) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null) return null;
        for (Way way : ds.getWays()) {
            if (way.getUniqueId() == wayId) return way;
        }
        return null;
    }

    /**
     * 根据唯一ID在当前数据集中查找节点
     * @param nodeId 节点唯一ID
     * @return 节点对象，未找到则返回null
     */
    private Node findNodeById(long nodeId) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null) return null;
        for (Node node : ds.getNodes()) {
            if (node.getUniqueId() == nodeId) return node;
        }
        return null;
    }

    /**
     * 清除所有高亮并还原窗口打开时的选中要素
     */
    private void clearHighlight() {
        highlightHelper.clear();
        HighlightHelper.clearAllHighlighted();
        // 还原窗口打开时的选中要素
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds != null) {
            ds.setSelected(savedSelection);
        }
        if (MainApplication.getMap() != null) {
            MainApplication.getMap().repaint();
        }
    }

    /**
     * 根据当前下拉选择框的选中项刷新表格数据
     */
    private void refreshTable() {
        // 退出单元格编辑模式，将编辑器中的值提交到表格模型
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        // 保存当前表格中的编辑
        saveCurrentTableEdits();

        WayComboItem selected = (WayComboItem) wayComboBox.getSelectedItem();
        if (selected == null) return;

        // 确定要显示的拐角索引列表
        if (selected.isAll) {
            cornerIdxDisplaying = new ArrayList<>();
            for (int i = 0; i < allCorners.size(); i++) {
                cornerIdxDisplaying.add(i);
            }
        } else {
            cornerIdxDisplaying = new ArrayList<>(
                    cornerIdxMap.getOrDefault(selected.wayId, new ArrayList<>())
            );
        }

        // 清空表格并重新填充
        tableModel.setRowCount(0);
        for (int idx : cornerIdxDisplaying) {
            CornerInfo c = allCorners.get(idx);
            tableModel.addRow(new Object[]{
                    String.valueOf(c.wayId),
                    c.cornerNoInWay,
                    String.valueOf(c.nodeId),
                    Math.round(c.angleDeg * 100.0) / 100.0,
                    Math.round(c.preferredMaxRadius * 100.0) / 100.0,
                    Math.round(editedRadii[idx] * 100.0) / 100.0
            });
        }
    }

    /**
     * 将拐角列表按路径ID分组，记录各拐角在allCorners中的索引
     * @param corners 所有拐角信息列表
     * @return 路径ID到拐角索引列表的映射
     */
    private static Map<Long, List<Integer>> groupCornerIdxByWayId(List<CornerInfo> corners) {
        Map<Long, List<Integer>> map = new LinkedHashMap<>();
        for (int i = 0; i < corners.size(); i++) {
            CornerInfo c = corners.get(i);
            map.computeIfAbsent(c.wayId, k -> new ArrayList<>()).add(i);
        }
        return map;
    }

    private JScrollPane makeAndGetTable() {
        String[] columnNames = {
                I18n.tr("Way ID"), I18n.tr("No."),
                I18n.tr("Node ID"),
                I18n.tr("Angle (°)"), I18n.tr("Preferred Max Radius (m)"),
                I18n.tr("Setted Radius (m)")
        };

        // 初始显示全部拐角
        cornerIdxDisplaying = new ArrayList<>();
        for (int i = 0; i < allCorners.size(); i++) {
            cornerIdxDisplaying.add(i);
        }

        Object[][] data = buildTableDataForIndices(cornerIdxDisplaying);
        tableModel = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;  // 只有「半径」列可编辑
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 2) return String.class;  // Way ID、Node ID为字符串
                return Double.class;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(600, 250));
        // 设置列宽
        int totalWidth = tableScrollPane.getPreferredSize().width;
        double[] percentages = {0.10, 0.05, 0.10, 0.15, 0.30, 0.30};
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth((int)(totalWidth * percentages[i]));
        }

        // 列表选中行变更时更新高亮
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateHighlight();
            }
        });

        return tableScrollPane;
    }

    /**
     * 计算所有输入路径中的拐角信息
     * @param input 输入要素
     * @param params 输入参数（包含默认半径、最大最小角度等）
     * @return 拐角信息列表
     */
    private static List<CornerInfo> computeCorners(ColumbinaInput input, FilletParams params) {
        List<CornerInfo> corners = new ArrayList<>();
        if (input == null) return corners;

        for (Way way : input.getWays()) {
            corners.addAll(computeCornersForWay(way, params));
        }
        return corners;
    }

    /**
     * 计算单条路径上所有拐角
     * @param way 路径
     * @param params 输入参数（包含默认半径、最大最小角度等）
     * @return 拐角信息列表（No.从1开始递增）
     */
    private static List<CornerInfo> computeCornersForWay(Way way, FilletParams params) {
        List<CornerInfo> corners = new ArrayList<>();
        if (way == null) return corners;

        int numNode = way.isClosed() ? way.getNodesCount() - 1 : way.getNodesCount();
        if (numNode < 3) return corners;  // 至少3个节点才能形成拐角
        int endIdx = way.isClosed() ? numNode : numNode - 2;

        for (int i = 1; i <= endIdx; i++) {
            try {
                ColumbinaCorner corner = ColumbinaCorner.create(way, (i - 1 + numNode) % numNode);
                // 跳过近似直线的拐角和过于尖锐的拐角（由传入的params决定，也就是FilletDialog中用户点击高级时编辑框中的值）
                if (corner.angleRad > Math.toRadians(params.maxAngleDeg) || corner.angleRad < Math.toRadians(params.minAngleDeg))
                    continue;

                double surfaceLenBA = UtilsMath.eastNorthDistanceToSurface(corner.lenBA, corner.latB);
                double surfaceLenBC = UtilsMath.eastNorthDistanceToSurface(corner.lenBC, corner.latB);
                double maxRadius = UtilsMath.getMaxRadiusForCorner(surfaceLenBA, surfaceLenBC, corner.angleRad);

                Node cornerNode = way.getNode(i % numNode);
                corners.add(new CornerInfo(
                        way.getUniqueId(),
                        i,
                        cornerNode.getUniqueId(),
                        Math.toDegrees(corner.angleRad),
                        maxRadius
                ));
            } catch (Exception ignored) {
                // 跳过无法计算的拐角
            }
        }
        return corners;
    }

    /**
     * 根据拐角索引列表构建表格数据
     * @param indices 要显示的拐角在allCorners中的索引列表
     * @return 表格数据（6列：Way ID, No., Node ID, Angle, Preferred Max Radius, Radius）
     */
    private Object[][] buildTableDataForIndices(List<Integer> indices) {
        Object[][] data = new Object[indices.size()][6];
        for (int row = 0; row < indices.size(); row++) {
            int idx = indices.get(row);
            CornerInfo c = allCorners.get(idx);
            data[row][0] = String.valueOf(c.wayId);
            data[row][1] = c.cornerNoInWay;
            data[row][2] = String.valueOf(c.nodeId);
            data[row][3] = Math.round(c.angleDeg * 100.0) / 100.0;
            data[row][4] = Math.round(c.preferredMaxRadius * 100.0) / 100.0;
            data[row][5] = Math.round(editedRadii[idx] * 100.0) / 100.0;
        }
        return data;
    }

    /**
     * 获取高级参数，将表格中各路径各拐角的半径汇总
     * @return 高级圆角参数
     */
    public AdvFilletParams getAdvParams() {
        // 退出单元格编辑模式，将编辑器中的值提交到表格模型
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        // 保存当前表格中可能尚未同步的编辑
        saveCurrentTableEdits();

        Map<Long, List<Double>> radiusMap = new LinkedHashMap<>();

        // 按路径ID分组汇总各拐角的半径
        for (int i = 0; i < allCorners.size(); i++) {
            CornerInfo corner = allCorners.get(i);
            radiusMap.computeIfAbsent(corner.wayId, k -> new ArrayList<>()).add(editedRadii[i]);
        }

        return new AdvFilletParams(radiusMap);
    }

    /**
     * 下拉选择框的选项项
     */
    private static class WayComboItem {
        final boolean isAll;    // 是否为「全部」选项
        final long wayId;       // 路径ID（isAll为true时无意义）
        final String label;

        WayComboItem(boolean isAll, long wayId, String label) {
            this.isAll = isAll;
            this.wayId = wayId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * 拐角信息
     */
    private static class CornerInfo {
        final long wayId;            // 所属路径ID
        final int cornerNoInWay;     // 路径内拐角编号（从1开始）
        final long nodeId;           // 拐角节点ID
        final double angleDeg;       // 拐角角度（度）
        final double preferredMaxRadius;  // 推荐最大半径（m）

        CornerInfo(long wayId, int cornerNoInWay, long nodeId, double angleDeg, double preferredMaxRadius) {
            this.wayId = wayId;
            this.cornerNoInWay = cornerNoInWay;
            this.nodeId = nodeId;
            this.angleDeg = angleDeg;
            this.preferredMaxRadius = preferredMaxRadius;
        }
    }
}
