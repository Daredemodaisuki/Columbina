package yakxin.columbina.utils;

import org.openstreetmap.josm.command.*;
import org.openstreetmap.josm.data.osm.*;

import java.util.ArrayList;
import java.util.List;

public class UtilsData {
    public static final int NODE_NOT_FOUND = -1;
    public static final int SELF_INTERSECTION = -2;

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
}


