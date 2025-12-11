package yakxin.columbina.data.dto;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;

/**
 * 公共单组输出类，目前单组输出只考虑输出一条线的情况
 */
public final class ColumbinaSingleOutput {
    public final List<Node> newNodes;
    public final List<Long> failedNodes;

    public ColumbinaSingleOutput(List<Node> newNodes, List<Long> failedNodesInSingleInput) {
        this.newNodes = newNodes;
        this.failedNodes = failedNodesInSingleInput;
    }

    /**
     * 直接使用结果中的newNodes组成新线
     * @return 新线
     */
    public Way linkNodesToWay() {
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
