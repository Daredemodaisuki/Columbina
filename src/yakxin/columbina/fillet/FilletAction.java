package yakxin.columbina.fillet;

// JOSM GUI和数据处理类
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

// Utilsplugin2插件
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryCommand;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;

// 哥伦比娅.data
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.FilletResult;
import yakxin.columbina.data.preference.FilletPreference;
import yakxin.columbina.utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * 导圆角交互类
  */
public class FilletAction extends JosmAction {
    private static final Shortcut shortcutFillet = Shortcut.registerShortcut(
            "tools:filletCorners",
            "More tools: Columbina/Round corners",
            KeyEvent.VK_C,
            Shortcut.ALT_CTRL_SHIFT
    );

    /// 构建菜单实例（构造函数）
    public FilletAction() {
        // 调用父类构造函数设置动作属性
        super(
                I18n.tr("Round Corners"),  // 菜单显示文本
                "RoundCorners",  // 图标
                I18n.tr("Round corners of selected ways with specified radius."),  // 工具提示
                shortcutFillet,  // 快捷键
                true,  // 启用工具栏按钮
                false
        );
    }

    private LayerDatasetAndWaySlc getLayerDatasetAndWaySlc() {
        // Map<String, Object> result = new HashMap<>();

        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();  // 当前的编辑图层
        if (layer == null) throw new ColumbinaException(I18n.tr("Current layer is not available."));

        DataSet dataset = MainApplication.getLayerManager().getEditDataSet();  // 当前的编辑数据库
        if (dataset == null) throw new ColumbinaException(I18n.tr("Current dataset is not available."));

        List<Way> waySelection = new ArrayList<>();  // 当前选中路径
        for (OsmPrimitive p : layer.data.getSelected()) {
            if (p instanceof Way) waySelection.add((Way) p);
        }
        // List<Way> waySelection = new ArrayList<>(dataset.getSelectedWays());  // 未测试的方法
        if (waySelection.isEmpty()) throw new IllegalArgumentException(I18n.tr("No way is selected."));
        if (waySelection.size() > 5) {
            int confirmTooMany = JOptionPane.showConfirmDialog(
                    null,
                    I18n.tr("Are you sure you want to process {0} ways at once? This may take a long time.", waySelection.size()),
                    I18n.tr("Round Corners"),
                    JOptionPane.YES_NO_OPTION
            );
            if (confirmTooMany == JOptionPane.NO_OPTION) return null;
        }

        // result.put("layer", layer);
        // result.put("dataset", dataset);
        // result.put("selectedWays", waySelection);
        return new LayerDatasetAndWaySlc(layer, dataset, waySelection);
    }

    private FilletParams getParams() {
        // Map<String, Object> result = new HashMap<>();
        FilletDialog filletDialog = new FilletDialog();  // 创建设置对话框

        if (filletDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1

        double radius = filletDialog.getFilletRadius();  // 圆角半径
        if (radius <= 0.0) throw new IllegalArgumentException(I18n.tr("Invalid round corner radius, should be greater than 0m."));
        // result.put("radius", radius);

        double angleStep = filletDialog.getFilletAngleStep();  // 圆角步进
        if (angleStep < 0.1) {
            angleStep = 0.1;
            filletDialog.setFilletAngleStep(0.1);
            utils.warnInfo(I18n.tr("Minimum angle step for round corner should be at least 0.1°, set to 0.1°."));
        }
        else if (angleStep > 10.0) utils.warnInfo(I18n.tr("Angle step is too large, the result may not be good."));
        // result.put("angleStep", angleStep);

        int maxPointNum = filletDialog.getFilletMaxPointNum();  // 曲线点数
        if (maxPointNum < 1) throw new IllegalArgumentException(I18n.tr("Invalid maximum number of points for round corner, should be at least 1."));
        else if (maxPointNum < 5) utils.warnInfo(I18n.tr("Maximum number of points for round corner is too low, the result may not be ideal."));
        // result.put("maxPointNum", maxPointNum);

        double minAngleDeg = filletDialog.getMinAngleDeg();  // 最小张角
        if (minAngleDeg < 0.0) {
            minAngleDeg = 0.0;
            filletDialog.setMinAngleDeg(0.0);
            utils.warnInfo(I18n.tr("Minimum angle should be at least 0°, set to 0°."));
        }
        // result.put("minAngleDeg", minAngleDeg);

        double maxAngleDeg = filletDialog.getMaxAngleDeg();  // 最大张角
        if (maxAngleDeg > 180.0) {
            maxAngleDeg = 180.0;
            filletDialog.setMaxAngleDeg(180.0);
            utils.warnInfo(I18n.tr("Maximum angle should be at most 180°, set to 180°."));
        }
        // result.put("maxAngleDeg", maxAngleDeg);


        // 是否删除旧路径、选择新路径、复制旧路径标签
        // result.put("deleteOld", roundCornersDialog.getIfDeleteOld());
        // result.put("selectNew", roundCornersDialog.getIfSelectNew());
        // result.put("copyTag", roundCornersDialog.getIfCopyTag());

        // 保存设置
        FilletPreference.setPreferenceFromDialog(filletDialog);
        FilletPreference.savePreference();
        return new FilletParams(
                radius, angleStep, maxPointNum,
                minAngleDeg, maxAngleDeg,
                filletDialog.getIfDeleteOld(), filletDialog.getIfSelectNew(), filletDialog.getIfCopyTag()
        );
    }

    // 画一条线及其新节点的指令
    private NewNodeWayCmd getNewNodeWayCmd(
            DataSet ds, Way w,
            double radius, double angleStep, int pointNum,
            double minAngleDeg, double maxAngleDeg,
            boolean copyTag
    ) {
        // Map<String, Object> results = new HashMap<>();

        // 计算平滑路径，获取待画的新节点（按新路径节点顺序排列）
        FilletResult filletResult = FilletGenerator.buildSmoothPolyline(
                w,
                radius, angleStep, pointNum,
                minAngleDeg, maxAngleDeg);
        if (filletResult == null || filletResult.newNodes == null || filletResult.newNodes.size() < 2) {
            return null;
        }
        List<Node> newNodes = filletResult.newNodes;
        List<Long> failedNodeIds = filletResult.failedNodes;

        // 画新线
        Way newWay = new Way();
        for (Node n : newNodes) newWay.addNode(n);  // 向新路径添加所有新节点

        // 复制原Way标签
        if (copyTag) {
            Map<String, String> wayTags = w.getInterestingTags();  // 读取原Way的tag
            newWay.setKeys(wayTags);
        }

        // 正式构建绘制命令
        List<Command> addCommands = new LinkedList<>();
        for (Node n : newNodes.stream().distinct().toList()) {  // 路径内部可能有节点复用（如闭合线），去重
            if (!ds.containsNode(n))  // 新路径的节点在ds中未绘制（不是复用的）才准备绘制
                addCommands.add(new AddCommand(ds, n));  // 添加节点到命令序列
        }
        addCommands.add(new AddCommand(ds, newWay));  // 添加线到命令序列

        // results.put("newWay", newWay);
        // results.put("addCommands", addCommands);
        // results.put("failedNodeIds", failedNodeIds);
        return new NewNodeWayCmd(newWay, addCommands, failedNodeIds);
    }

    // 汇总全部添加指令
    public AddCommandsCollected addCommand(
            DataSet ds, List<Way> selectedWays,
            double radius, double angleStep, int maxPointNum,
            double minAngleDeg, double maxAngleDeg,
            boolean copyTag
    ) {
        // Map<String, Object> result = new HashMap<>();
        List<Command> commands = new ArrayList<>();
        Map<Way, Way> oldNewWayPairs = new HashMap<>();
        Map<Way, List<Long>> failedNodeIds = new HashMap<>();

        // 处理路径
        // List<Way> newWays = new ArrayList<>();
        for (Way w : selectedWays) {  // 分别处理每条路径
            try {  // 一条路径出错尽可能不影响其他的
                NewNodeWayCmd newNWCmd = getNewNodeWayCmd(
                        ds, w,
                        radius, angleStep, maxPointNum,
                        minAngleDeg, maxAngleDeg,
                        copyTag);

                if (newNWCmd != null) {  // TODO：检查会否和已提交但未执行（进入ds）的重复提交？
                    commands.addAll(newNWCmd.addCommands);
                    oldNewWayPairs.put(w, newNWCmd.newWay);
                    failedNodeIds.put(w, newNWCmd.failedNodeIds);
                }
                else utils.warnInfo(I18n.tr(
                        "Algorithm did not return at least 2 nodes to form a way for way {0}, this way was not processed.",
                        w.getUniqueId()
                ));
            } catch (Exception exAdd) {
                utils.errorInfo(I18n.tr("Unexpected error occurred while processing way {0}: {1}",
                        w.getUniqueId(), exAdd.getMessage()
                ));
            }
        }

        if (commands.isEmpty()) {  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));
        }
        // 去重防止提交重复添加
        commands = commands.stream().distinct().toList();

        // result.put("commands", commands);
        // result.put("oldNewWayPairs", oldNewWayPairs);
        // result.put("failedNodeIds", failedNodeIds);
        return new AddCommandsCollected(commands, oldNewWayPairs, failedNodeIds);
    }

    // 移除/替换一条旧路径及无用节点的指令
    public List<Command> getRemoveCmd(DataSet ds, Way oldWay, Way newWay) {
        List<Command> removeCommands = new ArrayList<>();

        // 既有路径替换
        if (oldWay.getId() != 0) {
            ReplaceGeometryCommand seqCmdRep = ReplaceGeometryUtils.buildReplaceWithNewCommand(oldWay, newWay);
            if (seqCmdRep == null) {
                utils.warnInfo(
                        I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                                + I18n.tr("The user canceled the replacement operation in the Utilsplugin2 window.\n\n")
                                + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId())
                );
                // "Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                // "用户在Utilsplugin2的窗口中取消了替换操作。\n"
                // "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。"
            }
            else {
                List<Command> cmdRep = utils.tryGetCommandsFromSeqCmd(seqCmdRep);
                removeCommands.addAll(cmdRep);
            }
        }
        // 新路径删除
        else {
            // List<Command> delCommands = new LinkedList<>();
            if (oldWay.getReferrers().isEmpty()) {  // 旧路径有关系，连同节点都不删
                removeCommands.add(new DeleteCommand(ds, oldWay));  // 去除路径
                // 去除节点（闭合曲线会删闭合点2次，自交路径也会导致重复删除，需要去重）
                // 不能用ds.ContainsNode(n)判断删没删，因为cmdDel还没提交，delCommands内部重复删除会炸
                for (Node n : oldWay.getNodes().stream().distinct().toList()) {
                    boolean canBeDeleted = !n.isTagged();  // 有tag不删
                    for (OsmPrimitive ref : n.getReferrers()) {
                        if (!(ref instanceof Way && ref.equals(oldWay))) {
                            canBeDeleted = false;  // 被其他路径或关系使用，不删
                            break;
                        }
                    }
                    if (canBeDeleted) removeCommands.add(new DeleteCommand(ds, n));
                }
            } else {
                utils.warnInfo(I18n.tr(
                        "Old way {0} is still referenced by relations, not removed.",
                        oldWay.getUniqueId()
                ));
            }
        }
        return removeCommands;
    }

    // 汇总全部移除指令
    public List<Command> removeCommand(DataSet ds, Map<Way, Way> oldNewWayPairs) {
        List<Command> commands = new ArrayList<>();
        for (Map.Entry<Way, Way> oldNewWayEntry: oldNewWayPairs.entrySet()) {
            Way oldWay = oldNewWayEntry.getKey();
            Way newWay = oldNewWayEntry.getValue();
            if (oldWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing an old way:\n\n")
                                + I18n.tr("Old way return value is abnormal (null), unable to get the old way.\n\n")
                                + I18n.tr("This way may not have been properly rounded or removed.")
                        // "移除某条旧路径时产生了内部错误：\n\n"
                        //         + "旧路径返回值异常（null），无法获取旧路径。"
                        //         + "该路径可能未被正确倒角或移除。"
                );
            if (newWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing old way {0}:\n\n", oldWay.getUniqueId())
                                + I18n.tr("New way return value is abnormal (null), unable to get the new way.\n\n")
                                + I18n.tr("Old way {0} may not have been properly rounded or removed.", oldWay.getUniqueId()
                        )
                        // "移除旧路径" + oldWay.getUniqueId() + "时产生了内部错误：\n\n"
                        //         + "新路径返回值异常（null），无法获取新路径。"
                        //         + "\n\n旧路径" + oldWay.getUniqueId() + "可能未被正确倒角或移除。"
                );

            try {
                List<Command> cmdRmv = getRemoveCmd(ds, oldWay, newWay);
                if (cmdRmv != null && !cmdRmv.isEmpty()) {
                    commands.addAll(cmdRmv);
                }
            } catch (ReplaceGeometryException | IllegalArgumentException exUtils2) {
                utils.warnInfo(I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                        + I18n.tr("Message from Utilsplugin2:\n{0}\n\n", exUtils2.getMessage())
                        + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId()));
                        // "Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                        // + "来自Utilsplugin2的消息：\n"
                        // + exUtils2.getMessage()
                        // + "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。"
            } catch (Exception exRmv) {
                utils.errorInfo(I18n.tr(
                        "Unexpected error occurred while removing way {0}: {1}",
                                oldWay.getUniqueId(),
                                exRmv.getMessage()
                ));
            }
        }

        // 去重防止提交重复删除
        commands = commands.stream().distinct().toList();
        return commands;
    }


    /**
     * 点击事件
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer layer; DataSet dataset; List<Way> selectedWays;
        double radius; double angleStep; int pointNum;
        double minAngleDeg; double maxAngleDeg;
        boolean deleteOld; boolean selectNew; boolean copyTag;

        // 检查部分
        try {
            final LayerDatasetAndWaySlc lyDsWs = getLayerDatasetAndWaySlc();
            if (lyDsWs == null) return;  // 用户取消操作
            // layer = (OsmDataLayer) lyDsWs.get("layer");
            // dataset = (DataSet) lyDsWs.get("dataset");
            // selectedWays = (List<Way>) lyDsWs.get("selectedWays");
            layer = lyDsWs.layer;
            dataset = lyDsWs.dataset;
            selectedWays = lyDsWs.selectedWays;

            // 倒角参数设置
            final FilletParams filletParams = getParams();
            if (filletParams == null) return;  // 用户取消操作
            radius = filletParams.radius;
            pointNum = filletParams.maxPointNum;
            angleStep = filletParams.angleStep;
            deleteOld = filletParams.deleteOld;
            selectNew = filletParams.selectNew;
            copyTag = filletParams.copyTag;
            minAngleDeg = filletParams.minAngleDeg;
            maxAngleDeg = filletParams.maxAngleDeg;
        } catch (ColumbinaException | IllegalArgumentException exCheck) {
            utils.errorInfo(exCheck.getMessage());
            return;
        }


        // 绘制新路径
        AddCommandsCollected cmdsAddAndWayPairs;
        List<Command> cmdsAdd;
        Map<Way, Way> oldNewWayPairs;
        Map<Way, List<Long>> failedNodeIds;
        try{
            cmdsAddAndWayPairs = addCommand(
                    dataset, selectedWays,
                    radius, angleStep, pointNum,
                    minAngleDeg, maxAngleDeg,
                    copyTag);
            cmdsAdd = cmdsAddAndWayPairs.commands;
            oldNewWayPairs = cmdsAddAndWayPairs.oldNewWayPairs;
            failedNodeIds = cmdsAddAndWayPairs.failedNodeIds;
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            utils.errorInfo(exAdd.getMessage());
            return;
        }
        String undoRedoInfo;
        if (selectedWays.size() == 1) undoRedoInfo = I18n.tr("Round corners of way {0}: {1}m", selectedWays.getFirst().getUniqueId(), radius);
        else if (selectedWays.size() <= 5) undoRedoInfo = I18n.tr("Round corners of way {0}: {1}m", selectedWays.stream().map(Way::getId).toList(), radius);
        else undoRedoInfo = I18n.tr("Round corners of {0} ways: {1}m", selectedWays.size(), radius);
        if (!cmdsAdd.isEmpty()) {
            Command cmdAdd = new SequenceCommand(undoRedoInfo, cmdsAdd);
            UndoRedoHandler.getInstance().add(cmdAdd);  // 正式提交执行到命令序列
        }

        // 有角未倒成功时提示
        if (failedNodeIds != null && !failedNodeIds.isEmpty()) {
            boolean hasFailedNodes = false;
            String failedInfo = I18n.tr("The following corner nodes could not be rounded due to too short distance to adjacent nodes or not meeting the angle restrictions: ");
            for (Map.Entry<Way, List<Long>> failedEntry : failedNodeIds.entrySet()) {
                if (failedEntry.getValue().isEmpty()) continue;
                failedInfo = failedInfo
                        + I18n.tr("\nWay") + failedEntry.getKey().getUniqueId()
                        + I18n.tr(": ") + failedEntry.getValue();
                hasFailedNodes = true;
            }
            if (hasFailedNodes) utils.warnInfo(failedInfo);
        }

        // 移除旧路径
        if (deleteOld) {
            try {
                List<Command> cmdsRmv = removeCommand(dataset, oldNewWayPairs);
                if (!cmdsRmv.isEmpty()) {  // 如果全部都没有删除/替换，cmdsRmv为空会错错爆;
                    Command cmdRmv = new SequenceCommand(I18n.tr("Remove original ways"), cmdsRmv);
                    UndoRedoHandler.getInstance().add(cmdRmv);
                }
            } catch (ColumbinaException | IllegalArgumentException | ReplaceGeometryException exRemove) {
                utils.warnInfo(exRemove.getMessage());
                // return;
            }
            // TODO:选中的旧路径之间有交点且不与其他路径连接时，因为现在删/换数条线是打包到一次undoRedo中同时操作，
            //  删way1时认为交点在way2上，way2认为在way1上，一起做这些交点不会被删除/替换，需要手动处理
        }

        // 选中新路径
        if (selectNew) {
            List<Way> newWays = new ArrayList<>();
            for (Map.Entry<Way, Way> oldNewWayEntry: oldNewWayPairs.entrySet()) newWays.add(oldNewWayEntry.getValue());
            dataset.setSelected(newWays);
        }
    }


    /// 数据类
    private static final class LayerDatasetAndWaySlc {
        public final OsmDataLayer layer;
        public final DataSet dataset;
        public final List<Way> selectedWays;

        LayerDatasetAndWaySlc(OsmDataLayer layer, DataSet dataset, List<Way> selectedWays) {
            this.layer = layer;
            this.dataset = dataset;
            this.selectedWays = selectedWays;
        }
    }

    private static final class FilletParams {
        public final double radius;
        public final double angleStep;
        public final int maxPointNum;
        public final double minAngleDeg;
        public final double maxAngleDeg;
        public final boolean deleteOld;
        public final boolean selectNew;
        public final boolean copyTag;

        FilletParams(
                double radius, double angleStep, int maxPointNum,
                double minAngleDeg, double maxAngleDeg,
                boolean deleteOld, boolean selectNew, boolean copyTag
        ) {
            this.radius = radius;
            this.angleStep = angleStep;
            this.maxPointNum = maxPointNum;
            this.minAngleDeg = minAngleDeg;
            this.maxAngleDeg = maxAngleDeg;
            this.deleteOld = deleteOld;
            this.selectNew = selectNew;
            this.copyTag = copyTag;
        }
    }

    private static final class NewNodeWayCmd {
        public final Way newWay;
        public final List<Command> addCommands;
        public final List<Long> failedNodeIds;

        NewNodeWayCmd(Way newWay, List<Command> addCommands, List<Long> failedNodeIds) {
            this.newWay = newWay;
            this.addCommands = addCommands;
            this.failedNodeIds = failedNodeIds;
        }
    }

    private static final class AddCommandsCollected {
        public final List<Command> commands;
        public final Map<Way, Way> oldNewWayPairs;
        public final Map<Way, List<Long>> failedNodeIds;

        AddCommandsCollected(List<Command> commands, Map<Way, Way> oldNewWayPairs, Map<Way, List<Long>> failedNodeIds) {
            this.commands = commands;
            this.oldNewWayPairs = oldNewWayPairs;
            this.failedNodeIds = failedNodeIds;
        }
    }
}


