package yakxin.columbina.utils;

import org.openstreetmap.josm.command.*;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryCommand;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.abstractClasses.AbstractParams;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.DrawingNewNodeResult;
import yakxin.columbina.data.dto.LayerDatasetAndWaySelected;
import yakxin.columbina.data.dto.NewNodeWayCommands;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UtilsData {
    /// 数据相关
    public static void wayReplaceNode(Way way, int index, Node newNode) {
        way.removeNode(way.getNode(index));
        way.addNode(index, newNode);
    }

    public static List<Command> tryGetCommandsFromSeqCmd (SequenceCommand seqCmd) {
        List<Command> commands = new ArrayList<>();
        for (PseudoCommand pc : seqCmd.getChildren()) {
            if (pc instanceof Command) {
                commands.add((Command) pc);
            }
        }
        return commands;
    }

    /**
     * 获取选中的图层、数据库和路径
     * 如果图层、数据库有问题或没有选中路径，将抛出IllegalArgumentException异常
     * @return 获取到的图层、数据库和路径
     */
    public static LayerDatasetAndWaySelected getLayerDatasetAndWaySelected() {
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
                    I18n.tr("Columbina"),
                    JOptionPane.YES_NO_OPTION
            );
            if (confirmTooMany == JOptionPane.NO_OPTION) return null;
        }

        return new LayerDatasetAndWaySelected(layer, dataset, waySelection);
    }

    /**
     * 移除/替换一条旧路径及无用节点的指令，删多条把这个函数放在for里面一个个删
     * 移除/替换操作是跨功能统一的，所以统一这里实现
     * 添加节点和路径每个功能添加得不同，所以每个action类自行实现
     * @param ds 当前数据库
     * @param oldWay 希望移除的旧路径
     * @param newWay 用于替换的新路径
     * @return 指令列表
     */
    public static List<Command> getRemoveCmd(DataSet ds, Way oldWay, Way newWay) {
        List<Command> removeCommands = new ArrayList<>();

        // 既有路径替换
        if (oldWay.getId() != 0) {
            ReplaceGeometryCommand seqCmdRep = ReplaceGeometryUtils.buildReplaceWithNewCommand(oldWay, newWay);
            if (seqCmdRep == null) {
                UtilsUI.warnInfo(
                        I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                                + I18n.tr("The user canceled the replacement operation in the Utilsplugin2 window.\n\n")
                                + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId())
                );
                // "Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                // "用户在Utilsplugin2的窗口中取消了替换操作。\n"
                // "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。"
            }
            else {
                List<Command> cmdRep = tryGetCommandsFromSeqCmd(seqCmdRep);
                removeCommands.addAll(cmdRep);
            }
        }
        // 新路径删除
        else {
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
                UtilsUI.warnInfo(I18n.tr(
                        "Old way {0} is still referenced by relations, not removed.",
                        oldWay.getUniqueId()
                ));
            }
        }
        return removeCommands;
    }

    /**
     * 各个Generator类的get汇总后的获得新增单条路径所需指令的方法
     */
    public static <GeneratorType extends AbstractGenerator<ParamType>, ParamType extends AbstractParams> NewNodeWayCommands getAddCmd (
            DataSet ds, Way way,
            GeneratorType generator, ParamType params,
            boolean copyTag) {
        // 调用生成传入的函数计算路径
        DrawingNewNodeResult filletResult = generator.getNewNodeWay(way, params);
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
            Map<String, String> wayTags = way.getInterestingTags();  // 读取原Way的tag
            newWay.setKeys(wayTags);
        }

        // 正式构建绘制命令
        List<Command> addCommands = new LinkedList<>();
        for (Node n : newNodes.stream().distinct().toList()) {  // 路径内部可能有节点复用（如闭合线），去重
            if (!ds.containsNode(n))  // 新路径的节点在ds中未绘制（不是复用的）才准备绘制
                addCommands.add(new AddCommand(ds, n));  // 添加节点到命令序列
        }
        addCommands.add(new AddCommand(ds, newWay));  // 添加线到命令序列

        return new NewNodeWayCommands(newWay, addCommands, failedNodeIds);
    }

}


