package yakxin.columbina.utils;

import org.openstreetmap.josm.command.*;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UtilsData {
    public static final int NODE_NOT_FOUND = -1;
    public static final int SELF_INTERSECTION = -2;

    /// 数据相关
    /**
     * 替换路径上的节点（仅供未提交对象使用）
     * @param way 路径
     * @param index 欲替换的节点的索引
     * @param newNode 新节点
     */
    public static void wayReplaceNode(Way way, int index, Node newNode) {
        List<Node> originalNodes = way.getNodes();
        originalNodes.set(index, newNode);
        way.setNodes(originalNodes);
    }

    /**
     * 从SequenceCommand中解出List<Command>
     * @param seqCmd SequenceCommand
     * @return List<Command>
     */
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
     * 获取非自交节点在路径中的位置
     * <p>其中：
     * <ul>
     *     <li>路径中不包含该节点时，返回NODE_NOT_FOUND（-1）</li>
     *     <li>如果是自交路径的自交点，返回SELF_INTERSECTION（-2）</li>
     *     <li>对于闭合路径的闭合点，返回0而不是最末尾的way.getNodesCount()-1</li>
     * </ul>
     * @param node 节点
     * @param way 路径
     * @return 位置
     */
    public static int getNodeIndex(Node node, Way way) {
        int nodeNum = way.isClosed() ? way.getNodesCount() - 1 : way.getNodesCount();  // 闭合曲线过滤闭合点
        int index = NODE_NOT_FOUND;
        for (int i = 0; i < nodeNum; i ++) {
            if (way.getNode(i).getUniqueId() == node.getUniqueId()) {
                if (index == NODE_NOT_FOUND) index = i;
                else return SELF_INTERSECTION;
            }
        }
        return index;
    }

    /**
     * 具体检查输入要素的数量是否合法
     * <p>这不是AbstractDrawingAction的抽象实现，而是方便各个具体动作类实现时调用本方法，填入限制值进行检查。
     * <p>如果不需要限制某类要素最大或最小数目（无下限〔可有可无〕或无上限），输入AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM（-1）；
     * <p>如果需要某类要素至少1个的，下限需要设置为1；
     * <p>如果强制不需要某种输入，其最大最小值均填入AbstractDrawingAction.NO_THIS_KIND_OF_INPUT（0）。
     * @see AbstractDrawingAction#checkInputNum(ColumbinaInput)
     * @param totalInput 全部输入要素
     * @param minNode 最小节点数（含）
     * @param maxNode 最大节点数（含）
     * @param minWay 最小路径数（含）
     * @param maxWay 最大路径数（含）
     * @throws IllegalArgumentException 输入数量不合法
     */
    public static void checkInputNum(
            ColumbinaInput totalInput,
            int minNode, int maxNode,
            int minWay, int maxWay
    ) throws IllegalArgumentException {
        int node = totalInput.getInputNum(Node.class), way = totalInput.getInputNum(Way.class);

        // 节点
        if (minNode != AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM && node < minNode) {
            if (node == 0)
                throw new IllegalArgumentException(I18n.tr("No node is selected, required at least {0}.", minNode));
            throw new IllegalArgumentException(I18n.tr("Too few nodes are selected, required at least {0}.", minNode));
        }
        if (maxNode != AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM && node > maxNode) {
            if (maxNode == AbstractDrawingAction.NO_THIS_KIND_OF_INPUT)
                throw new IllegalArgumentException(I18n.tr("No node should be selected."));
            throw new IllegalArgumentException(I18n.tr("Too many nodes are selected, required at most {0}.", maxNode));
        }

        // 路径
        if (minWay != AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM && way < minWay) {
            if (way == 0)
                throw new IllegalArgumentException(I18n.tr("No way is selected, required at least {0}.", minWay));
            throw new IllegalArgumentException(I18n.tr("Too few ways are selected, required at least {0}.", minWay));
        }
        if (maxWay != AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM && way > maxWay) {
            if (maxWay == AbstractDrawingAction.NO_THIS_KIND_OF_INPUT)
                throw new IllegalArgumentException(I18n.tr("No way should be selected."));
            throw new IllegalArgumentException(I18n.tr("Too many ways are selected, required at most {0}.", maxWay));
        }
    }
    
    public static String featureListToString(List<OsmPrimitive> features) {
        return features.stream().map(feature -> {
                    if (feature instanceof Node) return I18n.tr("Node {0}", feature.getUniqueId());
                    else if (feature instanceof Way) return I18n.tr("Way {0}", feature.getUniqueId());
                    else if (feature instanceof Relation) return I18n.tr("Relation {0}", feature.getUniqueId());
                    else return feature.getClass().toString() + feature.getUniqueId();
                }).collect(Collectors.joining(",", "[", "]"));
    }
}


