package yakxin.columbia;

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
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

// Utilsplugin2插件
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryCommand;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
import yakxin.columbia.data.FilletResult;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/// 导圆角
public class RoundCornersAction extends JosmAction {

    /// 添加菜单（构造函数）
    public RoundCornersAction() {
        // 调用父类构造函数设置动作属性
        super(
                "路径倒圆角",  // 菜单显示文本
                "temp_icon",  // 图标
                "对选定路径的每个拐角节点按指定半径倒圆角。",  // 工具提示
                null,  // 不指定快捷键  // TODO:快捷键
                false  // 不启用工具栏按钮
        );

        // 将动作添加到JOSM主菜单
        MainApplication.getMenu().moreToolsMenu.add(new JMenuItem(this));
    }

    /// 点击事件
    @Override
    public void actionPerformed(ActionEvent e) {
        /// 检查
        // 检查当前的编辑图层
        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
        if (layer == null) {
            (new Notification("Columbia\n\n当前图层不可用。")).setIcon(JOptionPane.ERROR_MESSAGE).show();
            return;  // 图层不可用，退出
        }
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();

        // 收集所有选中的way
        List<Way> selection = new ArrayList<>();
        for (OsmPrimitive p : layer.data.getSelected()) {
            if (p instanceof Way) selection.add((Way) p);
        }
        // 检查是否有选中的way
        if (selection.isEmpty()) {
            (new Notification("Columbia\n\n没有选中路径。")).setIcon(JOptionPane.ERROR_MESSAGE).show();
            return;  // 没有选中道路，退出
        }

        /// 圆角设置
        RoundCornersDialog dlg = new RoundCornersDialog();  /// 创建设置对话框
        // utils.testMsgWindow(dlg.getFilletRadius() + " " + dlg.getValue());
        if (dlg.getValue() != 1) return;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1
        // 输入半径
        double radius = dlg.getFilletRadius();
        if (radius <= 0.0) {
            (new Notification("Columbia\n\n路径倒圆角半径无效。")).setIcon(JOptionPane.ERROR_MESSAGE).show();
            return;  // 输入无效，退出
        }
        // 曲线点数
        int pointNum = dlg.getFilletPointNum();
        if (pointNum <= 0) {
            (new Notification("Columbia\n\n路径倒圆角曲线点数无效。")).setIcon(JOptionPane.ERROR_MESSAGE).show();
            return;  // 输入无效，退出
        } else if (pointNum < 5) {
            (new Notification("Columbia\n\n路径倒圆角曲线点数较少，效果可能不理想。")).setIcon(JOptionPane.WARNING_MESSAGE).show();
        }
        // 是否删除旧路径和选择新路径
        boolean deleteOld = dlg.getIfDeleteOld();
        boolean selectNew = dlg.getIfSelectNew();
        boolean copyTag = dlg.getIfCopyTag();

        /// 处理每条路径
        List<Way> newWays = new ArrayList<>();
        List<Long> failedNodes = new ArrayList<>();
        for (Way w : selection) {
            try {
                // 计算路径
                FilletResult filletResult = FilletGenerator.buildSmoothPolyline(w, radius, pointNum);
                List<Node> newNodes = filletResult.newNodes;  // 计算平滑路径
                if (newNodes != null && !newNodes.isEmpty()) {
                    // 创建新的圆角路径
                    Way newWay = new Way();
                    for (Node n : newNodes) newWay.addNode(n);  // 添加所有新节点
                    if (w.isClosed()) newWay.addNode(newNodes.get(0));  // 闭合曲线再次添加第0个节点
                    // 复制原Way标签
                    if (copyTag) {
                        Map<String, String> wayTags = w.getInterestingTags();  // 读取原Way的tag
                        newWay.setKeys(wayTags);
                    }

                    // TODO:记录上次的半径

                    // 正式绘制
                    List<Command> addCommands = new LinkedList<>();  // 指令
                    for (Node n : newNodes) {
                        // ds.addPrimitive(n);  // 直接动数据库
                        addCommands.add(new AddCommand(ds, n));  // 添加节点到命令序列
                    }
                    // ds.addPrimitive(newWay);
                    addCommands.add(new AddCommand(ds, newWay));  // 添加线到命令序列
                    // 执行到命令序列
                    Command cmdAdd = new SequenceCommand(
                            "对路径 " + (w.getId() != 0 ? String.valueOf(w.getId()) : w.getUniqueId()) + " 倒圆角：" + radius + "m",
                            addCommands);
                    UndoRedoHandler.getInstance().add(cmdAdd);

                    // 移除原路径
                    if (deleteOld) {
                        // 既有路径替换
                        if (w.getId() != 0) {
                            try {
                                ReplaceGeometryCommand cmdRep = ReplaceGeometryUtils.buildReplaceWithNewCommand(w, newWay);
                                if (cmdRep == null) {
                                    (new Notification(
                                            "Columbia尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                                                    + "用户在Utilsplugin2的窗口中取消了替换操作。\n"
                                                    + "\n\n旧路径" + w.getUniqueId() + "未被移除。"
                                    )).setIcon(JOptionPane.WARNING_MESSAGE).show();
                                }
                                else UndoRedoHandler.getInstance().add(cmdRep);
                            } catch (ReplaceGeometryException | IllegalArgumentException utils2Info) {
                                (new Notification(
                                        "Columbia尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                                                + "来自Utilsplugin2的消息：\n"
                                                + utils2Info.getMessage()
                                                + "\n\n旧路径" + w.getUniqueId() + "未被移除。"
                                )).setIcon(JOptionPane.WARNING_MESSAGE).show();
                            }
                        }
                        // 新路径删除
                        else {
                            List<Command> delCommands = new LinkedList<>();
                            delCommands.add(new DeleteCommand(ds, w));  // 去除路径
                            for (Node n : w.getNodes()) {  // 去除节点
                                boolean canBeDeleted = !n.isTagged();  // 有tag不删
                                for (OsmPrimitive ref : n.getReferrers()) {
                                    if (!(ref instanceof Way && ref.equals(w))) {
                                        canBeDeleted = false;  // 被其他路径或关系使用，不删
                                        break;
                                    }
                                }
                                if (canBeDeleted) delCommands.add(new DeleteCommand(ds, n));
                            }
                            Command cmdDel = new SequenceCommand("移除原有路径", delCommands);
                            UndoRedoHandler.getInstance().add(cmdDel);
                        }
                    }

                    // 其他
                    newWays.add(newWay);  // 记录新路径以供选中
                    failedNodes.addAll(filletResult.failedNodes);
                }
                else {
                    (new Notification(
                            "Columbia\n\n处理路径" + w.getUniqueId() + "倒圆角函数没有返回节点。"
                    )).setIcon(JOptionPane.ERROR_MESSAGE).show();
                }
                if (!failedNodes.isEmpty()) {  // 没有倒角成功时警告
                    (new Notification(
                            "Columbia\n\n下列拐点节点因与相邻节点距离过短未能倒角：\n"
                                     + failedNodes
                    )).setIcon(JOptionPane.WARNING_MESSAGE).show();
                }
            } catch (Exception ex) {  // 处理单个路径处理时的错误，不影响其他路径
                (new Notification(
                        "Columbia\n\n处理路径" + w.getUniqueId() + "时出现了错误：\n\n"
                                + ex.getMessage()
                                + "该路径可能未产生倒圆角结果或结果错误。"
                )).setIcon(JOptionPane.ERROR_MESSAGE).show();
            }
        }
        // 选中新路径
        if (selectNew) ds.setSelected(newWays);
    }
}


