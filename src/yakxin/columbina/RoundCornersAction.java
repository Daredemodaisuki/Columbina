package yakxin.columbina;

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
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.FilletResult;
import yakxin.columbina.data.Preference;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;

/// 导圆角
public class RoundCornersAction extends JosmAction {
    private static final Shortcut shortcutRoundCorners = Shortcut.registerShortcut(
            "tools:roundCorners",
            "More tools: Columbina/Round corners",
            KeyEvent.VK_C,
            Shortcut.ALT_CTRL_SHIFT
    );

    /// 添加菜单（构造函数）
    public RoundCornersAction() {
        // 调用父类构造函数设置动作属性
        super(
                "路径倒圆角",  // 菜单显示文本
                "RoundCorners",  // 图标
                "对选定路径的每个拐角节点按指定半径倒圆角。",  // 工具提示
                shortcutRoundCorners,  // 暂不指定快捷键  // TODO:快捷键
                true,  // 启用工具栏按钮
                false
        );
    }

    private Map<String, Object> getLayerDatasetAndWaySlc() {
        Map<String, Object> result = new HashMap<>();

        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();  // 当前的编辑图层
        if (layer == null) throw new ColumbinaException("当前图层不可用。");

        DataSet dataset = MainApplication.getLayerManager().getEditDataSet();  // 当前的编辑数据库
        if (dataset == null) throw new ColumbinaException("当前数据库不可用。");

        List<Way> waySelection = new ArrayList<>();  // 当前选中路径
        for (OsmPrimitive p : layer.data.getSelected()) {
            if (p instanceof Way) waySelection.add((Way) p);
        }
        // List<Way> waySelection = new ArrayList<>(dataset.getSelectedWays());  // 未测试的方法
        if (waySelection.isEmpty()) throw new IllegalArgumentException("当前未选中路径。");
        if (waySelection.size() > 5) {
            int confirmTooMany = JOptionPane.showConfirmDialog(
                    null,
                    "真的需要一次性为" + waySelection.size() + "条路径进行操作？可能要花老长时间了。",
                    "路径倒圆角",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirmTooMany == JOptionPane.NO_OPTION) return null;
        }

        result.put("layer", layer);
        result.put("dataset", dataset);
        result.put("selectedWays", waySelection);
        return result;
    }

    private Map<String, Object> getParams() {
        Map<String, Object> result = new HashMap<>();
        RoundCornersDialog dialog = new RoundCornersDialog();  // 创建设置对话框

        if (dialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1

        double radius = dialog.getFilletRadius();  // 圆角半径
        if (radius <= 0.0) throw new IllegalArgumentException("路径倒圆角半径无效。");
        result.put("radius", radius);

        int pointNum = dialog.getFilletPointNum();  // 曲线点数
        if (pointNum <= 0) throw new IllegalArgumentException("路径倒圆角曲线点数无效。");
        else if (pointNum < 5) utils.warnInfo("路径倒圆角曲线点数较少，效果可能不理想。");
        result.put("pointNum", pointNum);

        // 是否删除旧路径、选择新路径、复制旧路径标签
        result.put("deleteOld", dialog.getIfDeleteOld());
        result.put("selectNew", dialog.getIfSelectNew());
        result.put("copyTag", dialog.getIfCopyTag());

        // 保存设置
        Preference.setPreferenceFromDialog(dialog);
        Preference.savePreference();
        return result;
    }

    // 画一条线及其新节点的指令
    private Map<String, Object> getNewNodeWayCmd(DataSet ds, Way w, double radius, int pointNum, boolean copyTag) {
        Map<String, Object> results = new HashMap<>();

        // 计算平滑路径，获取待画的新节点（按新路径节点顺序排列）
        FilletResult filletResult = FilletGenerator.buildSmoothPolyline(w, radius, pointNum);
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

        results.put("newWay", newWay);
        results.put("addCommands", addCommands);
        results.put("failedNodeIds", failedNodeIds);
        return results;
    }

    // 汇总全部添加指令
    public Map<String, Object> addCommand(DataSet ds, List<Way> selectedWays, double radius, int pointNum, boolean copyTag) {
        Map<String, Object> result = new HashMap<>();
        List<Command> commands = new ArrayList<>();
        Map<Way, Way> oldNewWayPairs = new HashMap<>();
        Map<Way, List<Long>> failedNodeIds = new HashMap<>();

        // 处理路径
        // List<Way> newWays = new ArrayList<>();
        for (Way w : selectedWays) {  // 分别处理每条路径
            try {  // 一条路径出错尽可能不影响其他的
                Map<String, Object> newNWCmd = getNewNodeWayCmd(ds, w, radius, pointNum, copyTag);

                if (newNWCmd != null) {  // TODO：检查会否和已提交但未执行（进入ds）的重复提交？
                    commands.addAll((List<Command>) newNWCmd.get("addCommands"));
                    oldNewWayPairs.put(w, (Way) newNWCmd.get("newWay"));
                    failedNodeIds.put(w, (List<Long>) newNWCmd.get("failedNodeIds"));
                }
                else utils.warnInfo("处理路径" + w.getUniqueId() + "时算法没有返回足以构成路径的至少2个节点，该路径未处理。");
            } catch (Exception exAdd) {
                utils.errorInfo("处理路径" + w.getUniqueId() + "时产生了意外错误：" + exAdd.getMessage());
            }
        }

        if (commands.isEmpty()) {  // 未能成功生成一条线
            throw new ColumbinaException("未能成功生成一条新路径。");
        }
        // 去重防止提交重复添加
        commands = commands.stream().distinct().toList();

        result.put("commands", commands);
        result.put("oldNewWayPairs", oldNewWayPairs);
        result.put("failedNodeIds", failedNodeIds);
        return result;
    }

    // 移除/替换一条旧路径及无用节点的指令
    public List<Command> getRemoveCmd(DataSet ds, Way oldWay, Way newWay) {
        List<Command> removeCommands = new ArrayList<>();

        // 既有路径替换
        if (oldWay.getId() != 0) {
            ReplaceGeometryCommand seqCmdRep = ReplaceGeometryUtils.buildReplaceWithNewCommand(oldWay, newWay);
            if (seqCmdRep == null) {
                utils.warnInfo("Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                        + "用户在Utilsplugin2的窗口中取消了替换操作。\n"
                        + "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。");
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
                utils.warnInfo("旧路径" + oldWay.getUniqueId() + "仍被关系引用，未移除。");
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
                        "移除某条旧路径时产生了内部错误：\n\n"
                                + "旧路径返回值异常（null），无法获取旧路径。"
                                + "该路径可能未被正确倒角或移除。"
                );
            if (newWay == null)
                throw new ColumbinaException(
                        "移除旧路径" + oldWay.getUniqueId() + "时产生了内部错误：\n\n"
                                + "新路径返回值异常（null），无法获取新路径。"
                                + "\n\n旧路径" + oldWay.getUniqueId() + "可能未被正确倒角或移除。"
                );

            try {
                List<Command> cmdRmv = getRemoveCmd(ds, oldWay, newWay);
                if (cmdRmv != null && !cmdRmv.isEmpty()) {
                    commands.addAll(cmdRmv);
                }
            } catch (ReplaceGeometryException | IllegalArgumentException exUtils2) {
                utils.warnInfo("Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                        + "来自Utilsplugin2的消息：\n"
                        + exUtils2.getMessage()
                        + "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。");
            } catch (Exception exRmv) {
                utils.errorInfo("移除路径" + oldWay.getUniqueId() + "时产生了意外错误：" + exRmv.getMessage());
            }
        }

        // 去重防止提交重复删除
        commands = commands.stream().distinct().toList();
        return commands;
    }


    /// 点击事件
    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer layer; DataSet dataset; List<Way> selectedWays;
        double radius; int pointNum;
        boolean deleteOld; boolean selectNew; boolean copyTag;

        // 检查部分
        try {
            final Map<String, Object> lyDsWs = getLayerDatasetAndWaySlc();
            if (lyDsWs == null) return;  // 用户取消操作
            layer = (OsmDataLayer) lyDsWs.get("layer");
            dataset = (DataSet) lyDsWs.get("dataset");
            selectedWays = (List<Way>) lyDsWs.get("selectedWays");

            // 倒角参数设置
            final Map<String, Object> params = getParams();
            if (params == null) return;  // 用户取消操作
            radius = (double) params.get("radius");
            pointNum = (int) params.get("pointNum");
            deleteOld = (boolean) params.get("deleteOld");
            selectNew = (boolean) params.get("selectNew");
            copyTag = (boolean) params.get("copyTag");
        } catch (ColumbinaException | IllegalArgumentException exCheck) {
            utils.errorInfo(exCheck.getMessage());
            return;
        }


        // 绘制新路径
        Map<String, Object> cmdsAddAndWayPairs;
        List<Command> cmdsAdd;
        Map<Way, Way> oldNewWayPairs;
        Map<Way, List<Long>> failedNodeIds;
        try{
            cmdsAddAndWayPairs = addCommand(dataset, selectedWays, radius, pointNum, copyTag);
            cmdsAdd = (List<Command>) cmdsAddAndWayPairs.get("commands");
            oldNewWayPairs = (Map<Way, Way>) cmdsAddAndWayPairs.get("oldNewWayPairs");
            failedNodeIds = (Map<Way, List<Long>>) cmdsAddAndWayPairs.get("failedNodeIds");
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            utils.errorInfo(exAdd.getMessage());
            return;
        }
        String undoRedoInfo;
        if (selectedWays.size() == 1) undoRedoInfo = "对路径" + selectedWays.getFirst().getUniqueId() + "倒圆角：" + radius + "m";
        else if (selectedWays.size() <= 5) undoRedoInfo = "对路径" + selectedWays.stream().map(Way::getId).toList() + "倒圆角：" + radius + "m";
        else undoRedoInfo = "对" + selectedWays.size() + "条路径倒圆角：" + radius + "m";
        if (!cmdsAdd.isEmpty()) {
            Command cmdAdd = new SequenceCommand(undoRedoInfo, cmdsAdd);
            UndoRedoHandler.getInstance().add(cmdAdd);  // 正式提交执行到命令序列
        }

        // 有角未倒成功时提示
        if (failedNodeIds != null && !failedNodeIds.isEmpty()) {
            boolean hasFailedNodes = false;
            String failedInfo = "下列拐点节点因与相邻节点距离过短未能倒角：";
            for (Map.Entry<Way, List<Long>> failedEntry : failedNodeIds.entrySet()) {
                if (failedEntry.getValue().isEmpty()) continue;
                failedInfo = failedInfo
                        + "\n路径" + failedEntry.getKey().getUniqueId()
                        + "：" + failedEntry.getValue();
                hasFailedNodes = true;
            }
            if (hasFailedNodes) utils.warnInfo(failedInfo);
        }

        // 移除旧路径
        if (deleteOld) {
            try {
                List<Command> cmdsRmv = removeCommand(dataset, oldNewWayPairs);
                if (!cmdsRmv.isEmpty()) {  // 如果全部都没有删除/替换，cmdsRmv为空会错错爆;
                    Command cmdRmv = new SequenceCommand("移除原有路径", cmdsRmv);
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
}


