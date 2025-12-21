package yakxin.columbina.data.dto;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.ColumbinaException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公共单组输出类，目前单组输出只考虑输出一条路径的情况
 */
public final class ColumbinaSingleOutput {
    public final List<Node> wayNodes;
    public final List<Map<Node, ColumbinaEN>> movingNodes;
    public final List<Long> failedNodes;
    public Map<String, Object> extraData;

    /**
     * 构造函数
     * @param newNodesInSingleOutput 单组输入产生的一条新路径的所有节点（包括数据集中已有的节点，由action类创建命令序列时去重和去已有）
     * @param failedNodeIdsInSingleInput 单组输入中部分失败的输入节点
     */
    public ColumbinaSingleOutput(List<Node> newNodesInSingleOutput, List<Long> failedNodeIdsInSingleInput) {
        this(newNodesInSingleOutput, new ArrayList<>(), failedNodeIdsInSingleInput, new HashMap<>());
    }
    public ColumbinaSingleOutput(List<Node> wayNodesInSingleOutput, List<Long> failedNodeIdsInSingleInput, Map<String, Object> extraData) {
        this(wayNodesInSingleOutput, new ArrayList<>(), failedNodeIdsInSingleInput, extraData);
    }
    public ColumbinaSingleOutput(List<Node> wayNodesInSingleOutput, List<Map<Node, ColumbinaEN>> movingNodes, List<Long> failedNodeIdsInSingleInput, Map<String, Object> extraData) {
        this.wayNodes = wayNodesInSingleOutput;
        this.movingNodes = movingNodes;
        this.failedNodes = failedNodeIdsInSingleInput;
        this.extraData = extraData;
    }

    /**
     * 直接使用结果中的newNodes组成新线
     * @return 新线
     */
    public Way linkNodesToWay() {
        if (! isValid()) return null;
        Way newWay = new Way();
        for (Node n : wayNodes) newWay.addNode(n);  // 向新路径添加所有新节点
        return newWay;
    }

    public boolean isValid() {
        return !(wayNodes == null);
    }

    public boolean ifCanMakeAWay() {
        return isValid() && wayNodes.size() >= 2;
    }
    
    public List<Command> toCommands(DataSet ds) {
        List<Command> result = new ArrayList<>();
        if (wayNodes != null && wayNodes.size() > 2) {
            for (Node n : wayNodes.stream().distinct().collect(Collectors.toList())) {  // 路径内部可能有节点复用（如闭合线），去重
                if (!ds.containsNode(n))  // 新路径的节点在ds中未绘制（不是复用的）才准备绘制
                    result.add(new AddCommand(ds, n));  // 添加节点到命令序列
            }
            
            Way newWay = linkNodesToWay();
            if (newWay != null) {
                result.add(new AddCommand(ds, newWay));  // 添加线到命令序列
            }
            
            return result;
        }
        throw new ColumbinaException("ColumbinaSingleOutput.toCommands: Filed to generate commands.");
    }
}


