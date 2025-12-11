package yakxin.columbina.data.dto;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;

/**
 * 公共单组输出类，目前单组输出只考虑输出一条路径的情况
 */
public final class ColumbinaSingleOutput {
    public final List<Node> newNodes;
    public final List<Long> failedNodes;

    /**
     * 构造函数
     * @param newNodesInSingleOutput 单组输入产生的一条新路径的所有节点（包括数据集中已有的节点，由action类创建命令序列时去重和去已有）
     * @param failedNodeIdsInSingleInput 单组输入中部分失败的输入节点
     */
    public ColumbinaSingleOutput(List<Node> newNodesInSingleOutput, List<Long> failedNodeIdsInSingleInput) {
        this.newNodes = newNodesInSingleOutput;
        this.failedNodes = failedNodeIdsInSingleInput;
    }

    /**
     * 直接使用结果中的newNodes组成新线
     * @return 新线
     */
    public Way linkNodesToWay() {
        if (! isValid()) return null;
        Way newWay = new Way();
        for (Node n : newNodes) newWay.addNode(n);  // 向新路径添加所有新节点
        return newWay;
    }

    public boolean isValid() {
        return !(newNodes == null);
    }

    public boolean ifCanMakeAWay() {
        return isValid() && newNodes.size() >= 2;
    }
}


