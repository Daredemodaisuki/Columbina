package yakxin.columbina.data.dto.outputs;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公共单组输出类，目前单组输出只考虑输出一条路径的情况
 */
public final class ColumbinaSingleOutput {
    public final List<ColumbinaOutputIntent<?>> outputIntents;
    public final List<OsmPrimitive> representatives;  // 这组输出中的代表性要素，可用于选中、字符串显示等
    
    public final List<OsmPrimitive> partialFailureInputs;
    public Map<String, Object> extraData;

    /**
     * 构造函数
     * <p>此项保留以兼容老功能：老功能都是画新路径，传入是所绘制路径全部节点，这里默认转为添加节点并画路径的意图，同时以新绘制的路径为选中代表。
     * @param wayNodesInSingleOutput 单组输入产生的一条新路径的所有节点（包括数据集中已有的节点，由ColumbinaOutputIntent转命令时去除已有，由action类创建命令序列时去重）
     * @param failedNodeIdsInSingleInput 单组输入中部分失败的输入节点
     */
    public ColumbinaSingleOutput(List<Node> wayNodesInSingleOutput, List<OsmPrimitive> failedNodeIdsInSingleInput) {
        // 转为添加节点意图
        this.outputIntents = new ArrayList<>();
        for (Node newNode : wayNodesInSingleOutput)
            outputIntents.add(new ColumbinaOutputIntent.AddThisNodeIfOK(newNode));
        // 节点连线
        Way newWay = new Way();
        for (Node n : wayNodesInSingleOutput) newWay.addNode(n);  // 向新路径添加所有新节点
        this.outputIntents.add(new ColumbinaOutputIntent.AddThisWayIfOK(newWay));
        this.representatives = List.of(newWay);  // 默认以新产生的路径为代表
        
        this.partialFailureInputs = failedNodeIdsInSingleInput;
        this.extraData = new HashMap<>();  // 老功能不需要
    }
    /**
     * 构造函数（新API）
     * @param outputIntents 输出修改意图列表
     * @param representatives 用于选中、显示id的代表性要素（应当保证其最后必须会留在数据集中）
     * @param failedNodeIdsInSingleInput 单组输入中部分失败的输入（比如输入整条路径，并对路径每个节点操作，那么某个节点失败就是部分失败）
     * @param extraData 额外数据
     */
    public ColumbinaSingleOutput(
            List<ColumbinaOutputIntent<?>> outputIntents, List<OsmPrimitive> representatives,
            List<OsmPrimitive> failedNodeIdsInSingleInput, Map<String, Object> extraData
    ) {
        this.outputIntents = outputIntents;
        this.representatives = representatives;
        this.partialFailureInputs = failedNodeIdsInSingleInput;
        this.extraData = extraData;
    }

    // TODO：可能需要更好的isValid判断
    public boolean isValid() {
        return !(outputIntents == null || outputIntents.isEmpty());
    }
}


