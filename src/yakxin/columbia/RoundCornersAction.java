package yakxin.columbia;

// 导入JOSM GUI和数据处理类
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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/// 导圆角
public class RoundCornersAction extends JosmAction {

    /// 添加菜单（构造函数）
    public RoundCornersAction() {
        // 调用父类构造函数设置动作属性
        super(
                "路径倒圆角",  // 菜单显示文本
                "temp_icon",  // 图标
                "对选定线段每个拐角指定半径倒圆角。",  // 工具提示
                null,  // 不指定快捷键
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
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "当前图层不可编辑");
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
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "没有选中路径");
            return;  // 没有选中道路，退出
        }

        /// 圆角半径对话框
        String s = JOptionPane.showInputDialog(MainApplication.getMainFrame(), "圆角半径（m）：", "150");
        if (s == null) return;  // 点击取消，退出

        // 检查输入的半径值
        double radius;
        try {
            radius = Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "半径无效");
            return;  // 输入无效，退出
        }

        /// 处理每条路径
        for (Way w : selection) {
            try {
                // 生成平滑路径
                List<Node> newNodes = FilletGenerator.buildSmoothPolyline(w, radius, layer);

                if (newNodes != null && !newNodes.isEmpty()) {
                    // 创建新的圆角路径
                    Way newWay = new Way();
                    for (Node n : newNodes) newWay.addNode(n);  // 添加所有新节点

                    // TODO:复制原Way标签
                    // TODO:没有倒角成功时警告
                    // TODO:处理闭合路径
                    // TODO:绘制完后选择新路径
                    // TODO:是否替换原路径可选项
                    // TODO:每段曲线最大点数
                    // TODO:记录上次的半径

                    // 正式绘制
                    List<Command> cmds = new LinkedList<>();  // 指令
                    for (Node n : newNodes) {
//                        ds.addPrimitive(n);  // 直接动数据库
                        cmds.add(new AddCommand(ds, n));  // 添加节点到命令序列
                    }
//                    ds.addPrimitive(newWay);
                    cmds.add(new AddCommand(ds, newWay));  // 添加线到命令序列

                    Command c = new SequenceCommand(
                            "对路径 " + (w.getId() != 0 ? String.valueOf(w.getId()) : w.getUniqueId()) + " 倒圆角：" + radius + "m",
                            cmds);
                    UndoRedoHandler.getInstance().add(c);  // 执行到命令序列
                }
            } catch (Exception ex) {  // 处理单个路径处理时的错误，不影响其他路径
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "Error processing way: " + ex.getMessage());
            }
        }
    }
}