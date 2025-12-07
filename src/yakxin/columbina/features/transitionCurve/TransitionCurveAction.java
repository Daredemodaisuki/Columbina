package yakxin.columbina.features.transitionCurve;

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
import yakxin.columbina.data.preference.TransitionCurvePreference;
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsUI;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * 过渡曲线（缓和曲线）交互类
 */
public class TransitionCurveAction extends JosmAction {
    private static final Shortcut shortcutTransitionCurve = Shortcut.registerShortcut(
            "tools:transitionCurve",
            "More tools: Columbina/Transition Curve",
            KeyEvent.VK_T,
            Shortcut.ALT_CTRL_SHIFT
    );

    /**
     * 构建菜单实例（构造函数）
     */
    public TransitionCurveAction() {
        super(
                I18n.tr("Transition Curve"),
                "TransitionCurve",
                I18n.tr("Create Euler spiral transition curves between straight segments."),
                shortcutTransitionCurve,
                true,
                false
        );
    }

    private TransitionCurveParams getParams() {
        TransitionCurveDialog dialog = new TransitionCurveDialog();

        if (dialog.getValue() != 1) return null;  // 用户取消

        double radius = dialog.getTransitionRadius();
        if (radius <= 0) {
            throw new IllegalArgumentException(I18n.tr("Invalid curve radius, should be greater than 0m."));
        }

        double length = dialog.getTransitionLength();
        if (length <= 0) {
            throw new IllegalArgumentException(I18n.tr("Invalid transition curve length, should be greater than 0m."));
        }

        double chainage = dialog.getChainageLength();
        if (chainage <= 0) {
            throw new IllegalArgumentException(I18n.tr("Invalid chainage length, should be greater than 0m."));
        }

        // 保存设置
        TransitionCurvePreference.setPreferenceFromDialog(dialog);
        TransitionCurvePreference.savePreference();

        return new TransitionCurveParams(
                radius, length, chainage,
                dialog.getIfDeleteOld(), dialog.getIfSelectNew(), dialog.getIfCopyTag()
        );
    }

    // /**
    //  * 获取画一条线及其新节点的指令
    //  */
    // private NewNodeWayCommands getNewNodeWayCmd(
    //         DataSet ds, Way way,
    //         double surfaceRadius, double surfaceLength, double surfaceChainage,
    //         boolean copyTag
    // ) {
    //     // 生成过渡曲线路径
    //     DrawingNewNodeResult transitionResult = TransitionCurveGenerator.buildTransitionCurvePolyline(
    //             way, surfaceRadius, surfaceLength, surfaceChainage
    //     );
    //     if (transitionResult == null || transitionResult.newNodes == null || transitionResult.newNodes.size() < 2) {
    //         return null;
    //     }
    //     List<Node> newNodes = transitionResult.newNodes;
    //     List<Long> failedNodeIds = transitionResult.failedNodes;
//
    //     // 画新线
    //     Way newWay = new Way();
    //     for (Node n : newNodes) newWay.addNode(n);
//
    //     // 复制原Way标签
    //     if (copyTag) {
    //         Map<String, String> wayTags = way.getInterestingTags();
    //         newWay.setKeys(wayTags);
    //     }
//
    //     // 正式构建绘制命令
    //     List<Command> addCommands = new LinkedList<>();
    //     for (Node n : newNodes.stream().distinct().toList()) {  // 路径内部可能有节点复用（如闭合线），去重
    //         if (!ds.containsNode(n)) {  // 新路径的节点在ds中未绘制（不是复用的）才准备绘制
    //             addCommands.add(new AddCommand(ds, n));  // 添加节点到命令序列
    //         }
    //     }
    //     addCommands.add(new AddCommand(ds, newWay));
//
    //     return new NewNodeWayCommands(newWay, addCommands, failedNodeIds);
    // }

    // 汇总全部添加命令
    public AddCommandsCollected concludeAddCommands(
            DataSet ds, List<Way> selectedWays,
            double radius, double length, double chainage,
            boolean copyTag
    ) {
        List<Command> commands = new ArrayList<>();
        Map<Way, Way> oldNewWayPairs = new HashMap<>();
        Map<Way, List<Long>> failedNodeIds = new HashMap<>();

        // 处理路径
        TransitionCurveGenerator generator = new TransitionCurveGenerator(
                radius, length, chainage
        );
        for (Way w : selectedWays) {
            try {
                NewNodeWayCommands newNWCmd = UtilsData.getAddCmd(ds, w, generator, copyTag);

                if (newNWCmd != null) {
                    commands.addAll(newNWCmd.addCommands);
                    oldNewWayPairs.put(w, newNWCmd.newWay);
                    failedNodeIds.put(w, newNWCmd.failedNodeIds);
                } else {
                    UtilsUI.warnInfo(I18n.tr(
                            "Algorithm did not return at least 2 nodes to form a way for way {0}, this way was not processed.",
                            w.getUniqueId()
                    ));
                }
            } catch (Exception exAdd) {
                UtilsUI.errorInfo(I18n.tr("Unexpected error occurred while processing way {0}: {1}",
                        w.getUniqueId(), exAdd.getMessage()
                ));
            }
        }

        if (commands.isEmpty()) {
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));
        }

        commands = commands.stream().distinct().toList();
        return new AddCommandsCollected(commands, oldNewWayPairs, failedNodeIds);
    }

    // 汇总全部移除指令（复用UtilsData.getRemoveCmd）
    public List<Command> concludeRemoveCommands(DataSet ds, Map<Way, Way> oldNewWayPairs) {
        List<Command> commands = new ArrayList<>();
        for (Map.Entry<Way, Way> oldNewWayEntry : oldNewWayPairs.entrySet()) {
            Way oldWay = oldNewWayEntry.getKey();
            Way newWay = oldNewWayEntry.getValue();

            if (oldWay == null) {
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing an old way:\n\n")
                                + I18n.tr("Old way return value is abnormal (null), unable to get the old way.\n\n")
                                + I18n.tr("This way may not have been properly processed or removed.")
                );
            }

            if (newWay == null) {
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing old way {0}:\n\n", oldWay.getUniqueId())
                                + I18n.tr("New way return value is abnormal (null), unable to get the new way.\n\n")
                                + I18n.tr("Old way {0} may not have been properly processed or removed.", oldWay.getUniqueId())
                );
            }

            try {
                List<Command> cmdRmv = UtilsData.getRemoveCmd(ds, oldWay, newWay);
                if (!cmdRmv.isEmpty()) {
                    commands.addAll(cmdRmv);
                }
            } catch (ReplaceGeometryException | IllegalArgumentException exUtils2) {
                UtilsUI.warnInfo(I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                        + I18n.tr("Message from Utilsplugin2:\n{0}\n\n", exUtils2.getMessage())
                        + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId()));
            } catch (Exception exRmv) {
                UtilsUI.errorInfo(I18n.tr(
                        "Unexpected error occurred while removing way {0}: {1}",
                        oldWay.getUniqueId(),
                        exRmv.getMessage()
                ));
            }
        }

        commands = commands.stream().distinct().toList();
        return commands;
    }

    /**
     * 点击事件
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer layer;
        DataSet dataset;
        List<Way> selectedWays;
        double radius, length, chainage;
        boolean deleteOld, selectNew, copyTag;

        try {
            LayerDatasetAndWaySelected lyDsWs = UtilsData.getLayerDatasetAndWaySelected();
            if (lyDsWs == null) return;

            layer = lyDsWs.layer;
            dataset = lyDsWs.dataset;
            selectedWays = lyDsWs.selectedWays;

            TransitionCurveParams params = getParams();
            if (params == null) return;

            radius = params.radius;
            length = params.length;
            chainage = params.chainage;
            deleteOld = params.deleteOld;
            selectNew = params.selectNew;
            copyTag = params.copyTag;

        } catch (ColumbinaException | IllegalArgumentException exCheck) {
            UtilsUI.errorInfo(exCheck.getMessage());
            return;
        }

        // 绘制新路径
        AddCommandsCollected cmdsAddAndWayPairs;
        List<Command> cmdsAdd;
        Map<Way, Way> oldNewWayPairs;
        Map<Way, List<Long>> failedNodeIds;

        try {
            cmdsAddAndWayPairs = concludeAddCommands(
                    dataset, selectedWays, radius, length, chainage, copyTag
            );
            cmdsAdd = cmdsAddAndWayPairs.commands;
            oldNewWayPairs = cmdsAddAndWayPairs.oldNewWayPairs;
            failedNodeIds = cmdsAddAndWayPairs.failedNodeIds;
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            UtilsUI.errorInfo(exAdd.getMessage());
            return;
        }

        String undoRedoInfo;
        if (selectedWays.size() == 1) {
            undoRedoInfo = I18n.tr("Transition curve of way {0}: R={1}m, Ls={2}m",
                    selectedWays.getFirst().getUniqueId(), radius, length);
        } else if (selectedWays.size() <= 5) {
            undoRedoInfo = I18n.tr("Transition curve of way {0}: R={1}m, Ls={2}m",
                    selectedWays.stream().map(Way::getId).toList(), radius, length);
        } else {
            undoRedoInfo = I18n.tr("Transition curve of {0} ways: R={1}m, Ls={2}m",
                    selectedWays.size(), radius, length);
        }

        if (!cmdsAdd.isEmpty()) {
            Command cmdAdd = new ColumbinaSeqCommand(undoRedoInfo, cmdsAdd, "TransitionCurve");
            UndoRedoHandler.getInstance().add(cmdAdd);
        }

        // 提示未处理的节点
        if (failedNodeIds != null && !failedNodeIds.isEmpty()) {
            boolean hasFailedNodes = false;
            StringBuilder failedInfo = new StringBuilder(
                    I18n.tr("The following corner nodes could not be processed due to geometric constraints or calculation errors: ")
            );

            for (Map.Entry<Way, List<Long>> failedEntry : failedNodeIds.entrySet()) {
                if (failedEntry.getValue().isEmpty()) continue;
                failedInfo.append(I18n.tr("\nWay"))
                        .append(failedEntry.getKey().getUniqueId())
                        .append(I18n.tr(": "))
                        .append(failedEntry.getValue());
                hasFailedNodes = true;
            }

            if (hasFailedNodes) {
                UtilsUI.warnInfo(failedInfo.toString());
            }
        }

        // 移除旧路径
        if (deleteOld) {
            try {
                List<Command> cmdsRmv = concludeRemoveCommands(dataset, oldNewWayPairs);
                if (!cmdsRmv.isEmpty()) {
                    Command cmdRmv = new ColumbinaSeqCommand(I18n.tr("Columbina: Remove original ways"), cmdsRmv, "RemoveOldWays");
                    UndoRedoHandler.getInstance().add(cmdRmv);
                }
            } catch (ColumbinaException | IllegalArgumentException | ReplaceGeometryException exRemove) {
                UtilsUI.warnInfo(exRemove.getMessage());
            }
        }

        // 选中新路径
        if (selectNew) {
            List<Way> newWays = new ArrayList<>();
            for (Map.Entry<Way, Way> oldNewWayEntry : oldNewWayPairs.entrySet()) {
                newWays.add(oldNewWayEntry.getValue());
            }
            dataset.setSelected(newWays);
        }
    }

    private static final class TransitionCurveParams {
        public final double radius;
        public final double length;
        public final double chainage;
        public final boolean deleteOld;
        public final boolean selectNew;
        public final boolean copyTag;

        TransitionCurveParams(
                double radius, double length, double chainage,
                boolean deleteOld, boolean selectNew, boolean copyTag
        ) {
            this.radius = radius;
            this.length = length;
            this.chainage = chainage;
            this.deleteOld = deleteOld;
            this.selectNew = selectNew;
            this.copyTag = copyTag;
        }
    }
}