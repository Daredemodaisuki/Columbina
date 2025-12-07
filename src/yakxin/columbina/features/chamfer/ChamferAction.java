package yakxin.columbina.features.chamfer;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;

import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.ColumbinaSeqCommand;
import yakxin.columbina.data.dto.AddCommandsCollected;
import yakxin.columbina.data.dto.LayerDatasetAndWaySelected;
import yakxin.columbina.data.dto.NewNodeWayCommands;
import yakxin.columbina.data.preference.ChamferPreference;
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsUI;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class ChamferAction extends JosmAction {
    private static final Shortcut shortcutChamferCorner = Shortcut.registerShortcut(
            "tools:chamferCorners",
            "More tools: Columbina/Chamfer corners",
            KeyEvent.VK_X,
            Shortcut.ALT_CTRL_SHIFT
    );

    /**
     * 构建菜单实例（构造函数）
      */
    public ChamferAction() {
        // 调用父类构造函数设置动作属性
        super(
                I18n.tr("Chamfer Corners"),  // 菜单显示文本
                "ChamferCorners",  // 图标
                I18n.tr("Chamfer corners of selected ways with specified distances or angle."),  // 工具提示
                shortcutChamferCorner,  // 快捷键
                true,  // 启用工具栏按钮
                false
        );
    }

    /**
     * 弹出参数对话框并获取参数、完成后保存设置
     */
    private ChamferParams getParams() {
        ChamferDialog chamferDialog = new ChamferDialog();
        if (chamferDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1

        int mode = chamferDialog.getChamferMode();

        double distanceA = chamferDialog.getChamferDistanceA();
        if (distanceA <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer distance BA, should be greater than 0m."));

        double distanceC = chamferDialog.getChamferDistanceC();
        if (mode == ChamferGenerator.USING_DISTANCE) {
            if (distanceC <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer distance BC, should be greater than 0m."));
        }

        double angleADeg = chamferDialog.getChamferAngleADeg();
        if (mode == ChamferGenerator.USING_ANGLE_A) {
            if (angleADeg <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer angle A, should be greater than 0m."));
        }

        // 保存设置
        ChamferPreference.setPreferenceFromDialog(chamferDialog);
        ChamferPreference.savePreference();

        return new ChamferParams(
                mode,  // 模式
                distanceA, distanceC, angleADeg,
                // 是否删除旧路径、选择新路径、复制旧路径标签
                chamferDialog.getIfDeleteOld(), chamferDialog.getIfSelectNew(), chamferDialog.getIfCopyTag()
        );
    }

    /**
     * 获取画一条线及其新节点的指令
     * @param ds 当前数据集
     * @param way 输入路径
     * @param mode 模式：<code>chamferGenerator.USING_DISTANCE</code>或<code>chamferGenerator.USING_ANGLE_A</code>
     * @param surfaceDistanceA 拐点B到A的地表距离
     * @param surfaceDistanceC 拐点B到C的地表距离
     * @param angleADeg 切角A的角度
     * @param copyTag 是否复制标签
     * @return 指令列表
     */
    //private NewNodeWayCommands getNewNodeWayCmd(
    //        DataSet ds, Way way, int mode,
    //        double surfaceDistanceA, double surfaceDistanceC,
    //        double angleADeg,
    //        boolean copyTag
    //) {
    //    // 计算斜角路径
    //    DrawingNewNodeResult chamferResult = ChamferGenerator.buildChamferPolyline(
    //            way, mode,
    //            surfaceDistanceA, surfaceDistanceC,
    //            angleADeg
    //    );
    //    if (chamferResult == null || chamferResult.newNodes == null || chamferResult.newNodes.size() < 2) {
    //        return null;
    //    }
    //    List<Node> newNodes = chamferResult.newNodes;
    //    List<Long> failedNodeIds = chamferResult.failedNodes;
//
    //    // 画新线
    //    Way newWay = new Way();
    //    for (Node n : newNodes) newWay.addNode(n);  // 向新路径添加所有新节点
//
    //    // 复制原Way标签
    //    if (copyTag) {
    //        Map<String, String> wayTags = way.getInterestingTags();  // 读取原Way的tag
    //        newWay.setKeys(wayTags);
    //    }
//
    //    // 正式构建绘制命令
    //    List<Command> addCommands = new LinkedList<>();
    //    for (Node n : newNodes.stream().distinct().toList()) {  // 路径内部可能有节点复用（如闭合线），去重
    //        if (!ds.containsNode(n))  // 新路径的节点在ds中未绘制（不是复用的）才准备绘制
    //            addCommands.add(new AddCommand(ds, n));  // 添加节点到命令序列
    //    }
    //    addCommands.add(new AddCommand(ds, newWay));  // 添加线到命令序列
//
    //    return new NewNodeWayCommands(newWay, addCommands, failedNodeIds);
    //}

    /**
     * 汇总全部添加命令
     * @param ds
     * @param selectedWays
     * @param mode
     * @param surfaceDistanceA
     * @param surfaceDistanceC
     * @param angleADeg
     * @param copyTag
     * @return
     */
    public AddCommandsCollected concludeAddCommands(
            DataSet ds, List<Way> selectedWays, int mode,
            double surfaceDistanceA, double surfaceDistanceC,
            double angleADeg,
            boolean copyTag
    ) {
        List<Command> commands = new ArrayList<>();
        Map<Way, Way> oldNewWayPairs = new HashMap<>();
        Map<Way, List<Long>> failedNodeIds = new HashMap<>();

        // 处理路径
        ChamferGenerator generator = new ChamferGenerator(
                mode,
                surfaceDistanceA, surfaceDistanceC,
                angleADeg
        );
        for (Way w : selectedWays) {  // 分别处理每条路径
            try {  // 一条路径出错尽可能不影响其他的
                NewNodeWayCommands newNWCmd = UtilsData.getAddCmd(ds, w, generator, copyTag);

                if (newNWCmd != null) {  // TODO：检查会否和已提交但未执行（进入ds）的重复提交？
                    commands.addAll(newNWCmd.addCommands);
                    oldNewWayPairs.put(w, newNWCmd.newWay);
                    failedNodeIds.put(w, newNWCmd.failedNodeIds);
                } else UtilsUI.warnInfo(I18n.tr(
                        "Algorithm did not return at least 2 nodes to form a way for way {0}, this way was not processed.",
                        w.getUniqueId()
                ));
            } catch (Exception exAdd) {
                UtilsUI.errorInfo(I18n.tr("Unexpected error occurred while processing way {0}: {1}",
                        w.getUniqueId(), exAdd.getMessage()
                ));
            }
        }
        if (commands.isEmpty()) {  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));
        }
        // 去重防止提交重复添加
        commands = commands.stream().distinct().toList();

        return new AddCommandsCollected(commands, oldNewWayPairs, failedNodeIds);
    }

    // 汇总全部移除指令
    public List<Command> concludeRemoveCommands(DataSet ds, Map<Way, Way> oldNewWayPairs) {
        List<Command> commands = new ArrayList<>();
        for (Map.Entry<Way, Way> oldNewWayEntry: oldNewWayPairs.entrySet()) {
            Way oldWay = oldNewWayEntry.getKey();
            Way newWay = oldNewWayEntry.getValue();
            if (oldWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing an old way:\n\n")
                                + I18n.tr("Old way return value is abnormal (null), unable to get the old way.\n\n")
                                + I18n.tr("This way may not have been properly chamferred or removed.")
                        // "移除某条旧路径时产生了内部错误：\n\n"
                        //         + "旧路径返回值异常（null），无法获取旧路径。"
                        //         + "该路径可能未被正确倒角或移除。"
                );
            if (newWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing old way {0}:\n\n", oldWay.getUniqueId())
                                + I18n.tr("New way return value is abnormal (null), unable to get the new way.\n\n")
                                + I18n.tr("Old way {0} may not have been properly chamferred or removed.", oldWay.getUniqueId()
                        )
                        // "移除旧路径" + oldWay.getUniqueId() + "时产生了内部错误：\n\n"
                        //         + "新路径返回值异常（null），无法获取新路径。"
                        //         + "\n\n旧路径" + oldWay.getUniqueId() + "可能未被正确倒角或移除。"
                );

            try {
                List<Command> cmdRmv = UtilsData.getRemoveCmd(ds, oldWay, newWay);
                if (!cmdRmv.isEmpty()) {  // cmdRmv != null &&
                    commands.addAll(cmdRmv);
                }
            } catch (ReplaceGeometryException | IllegalArgumentException exUtils2) {
                UtilsUI.warnInfo(I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                        + I18n.tr("Message from Utilsplugin2:\n{0}\n\n", exUtils2.getMessage())
                        + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId()));
                // "Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                // + "来自Utilsplugin2的消息：\n"
                // + exUtils2.getMessage()
                // + "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。"
            } catch (Exception exRmv) {
                UtilsUI.errorInfo(I18n.tr(
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
        double distanceA, distanceC; double angleADeg;
        int mode;
        boolean deleteOld; boolean selectNew; boolean copyTag;

        try {
            final LayerDatasetAndWaySelected lyDsWs = UtilsData.getLayerDatasetAndWaySelected();
            if (lyDsWs == null) return;  // 用户取消操作
            layer = lyDsWs.layer;
            dataset = lyDsWs.dataset;
            selectedWays = lyDsWs.selectedWays;

            // 输入参数
            final ChamferParams chamferParams = getParams();
            if (chamferParams == null) return;  // 用户取消操作
            distanceA = chamferParams.distanceA;
            distanceC = chamferParams.distanceC;
            angleADeg = chamferParams.angleADeg;
            deleteOld = chamferParams.deleteOld;
            selectNew = chamferParams.selectNew;
            copyTag = chamferParams.copyTag;
            mode = chamferParams.mode;

        } catch (ColumbinaException | IllegalArgumentException exCheck) {
            UtilsUI.errorInfo(exCheck.getMessage());
            return;
        }

        // 绘制新路径
        AddCommandsCollected cmdsAddAndWayPairs;
        List<Command> cmdsAdd;
        Map<Way, Way> oldNewWayPairs;
        Map<Way, List<Long>> failedNodeIds;
        try{
            cmdsAddAndWayPairs = concludeAddCommands(
                    dataset, selectedWays, mode,
                    distanceA, distanceC,
                    angleADeg,
                    copyTag);
            cmdsAdd = cmdsAddAndWayPairs.commands;
            oldNewWayPairs = cmdsAddAndWayPairs.oldNewWayPairs;
            failedNodeIds = cmdsAddAndWayPairs.failedNodeIds;
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            UtilsUI.errorInfo(exAdd.getMessage());
            return;
        }
        String undoRedoInfo;
        if (mode == ChamferGenerator.USING_DISTANCE) {
            if (selectedWays.size() == 1) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}m", selectedWays.getFirst().getUniqueId(), distanceA, distanceC);
            else if (selectedWays.size() <= 5) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}m", selectedWays.stream().map(Way::getId).toList(), distanceA, distanceC);
            else undoRedoInfo = I18n.tr("Chamfer of {0} ways: {1}m, {2}m", selectedWays.size(), distanceA, distanceC);
        } else {
            if (selectedWays.size() == 1) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}°", selectedWays.getFirst().getUniqueId(), distanceA, angleADeg);
            else if (selectedWays.size() <= 5) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}°", selectedWays.stream().map(Way::getId).toList(), distanceA, angleADeg);
            else undoRedoInfo = I18n.tr("Chamfer of {0} ways: {1}m, {2}°", selectedWays.size(), distanceA, angleADeg);
        }

        if (!cmdsAdd.isEmpty()) {
            Command cmdAdd = new ColumbinaSeqCommand(undoRedoInfo, cmdsAdd, "ChamferCorners");
            UndoRedoHandler.getInstance().add(cmdAdd);  // 正式提交执行到命令序列
        }

        // 有角未倒成功时提示
        if (failedNodeIds != null && !failedNodeIds.isEmpty()) {
            boolean hasFailedNodes = false;
            String failedInfo = I18n.tr("The following corner nodes could not be chamferred due to too short distance to adjacent nodes or not meeting the angle restrictions: ");
            for (Map.Entry<Way, List<Long>> failedEntry : failedNodeIds.entrySet()) {
                if (failedEntry.getValue().isEmpty()) continue;
                failedInfo = failedInfo
                        + I18n.tr("\nWay") + failedEntry.getKey().getUniqueId()
                        + I18n.tr(": ") + failedEntry.getValue();
                hasFailedNodes = true;
            }
            if (hasFailedNodes) UtilsUI.warnInfo(failedInfo);
        }

        // 移除旧路径
        if (deleteOld) {
            try {
                List<Command> cmdsRmv = concludeRemoveCommands(dataset, oldNewWayPairs);
                if (!cmdsRmv.isEmpty()) {  // 如果全部都没有删除/替换，cmdsRmv为空会错错爆;
                    Command cmdRmv = new ColumbinaSeqCommand(I18n.tr("Columbina: Remove original ways"), cmdsRmv, "RemoveOldWays");
                    UndoRedoHandler.getInstance().add(cmdRmv);
                }
            } catch (ColumbinaException | IllegalArgumentException | ReplaceGeometryException exRemove) {
                UtilsUI.warnInfo(exRemove.getMessage());
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

    private static final class ChamferParams {
        public final int mode;
        public final double distanceA;
        public final double distanceC;
        public final double angleADeg;
        public final boolean deleteOld;
        public final boolean selectNew;
        public final boolean copyTag;

        ChamferParams(
                int mode,
                double distanceA, double distanceC,
                double angleADeg,
                boolean deleteOld, boolean selectNew, boolean copyTag
        ) {
            this.mode = mode;
            this.distanceA = distanceA;
            this.distanceC = distanceC;
            this.angleADeg = angleADeg;
            this.deleteOld = deleteOld;
            this.selectNew = selectNew;
            this.copyTag = copyTag;
        }
    }
}


