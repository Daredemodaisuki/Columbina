package yakxin.columbina.data.dto.outputs;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.utils.UtilsData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公共单组输出类，目前单组输出只考虑输出一条路径的情况
 */
public final class ColumbinaSingleOutput {
    public final List<ColumbinaOutputIntent<?>> outputIntents;
    public final List<OsmPrimitive> representatives;  // 这组输出中的代表性要素，可用于选中、字符串显示等
    
    public final StatusEnum status;
    public final Map<OsmPrimitive, String> partiallyFailedInputs;  // 部分失败信息
    public final String generalInfo;  // 总体信息，包括单组输入完全失败信息或单组输入的整体警告信息
    
    public Map<String, Object> extraData;  // 额外信息（比如额外info）
    
    public enum StatusEnum {
        SUCCESSFUL, PARTIALLY_FAILED, FAILED
    }

    /**
     * 构造函数
     * <p>此项保留以兼容老功能：老功能都是画新路径，传入是所绘制路径全部节点，这里默认转为添加节点并画路径的意图，同时以新绘制的路径为选中代表。
     * @param wayNodesInSingleOutput 单组输入产生的一条新路径的所有节点（包括数据集中已有的节点，由ColumbinaOutputIntent转命令时去除已有，由action类创建命令序列时去重）
     * @param failedNodeIdsInSingleInput 单组输入中部分失败的输入节点
     */
    public ColumbinaSingleOutput(List<Node> wayNodesInSingleOutput, Map<OsmPrimitive, String> failedNodeIdsInSingleInput) {
        // 转为添加节点意图
        this.outputIntents = new ArrayList<>();
        for (Node newNode : wayNodesInSingleOutput)
            outputIntents.add(new ColumbinaOutputIntent.AddThisNodeIfOK(newNode));
        // 节点连线
        Way newWay = new Way();
        for (Node n : wayNodesInSingleOutput) newWay.addNode(n);  // 向新路径添加所有新节点
        this.outputIntents.add(new ColumbinaOutputIntent.AddThisWayIfOK(newWay));
        this.representatives = List.of(newWay);  // 默认以新产生的路径为代表
        
        this.status = failedNodeIdsInSingleInput != null && !failedNodeIdsInSingleInput.isEmpty() ?
                StatusEnum.PARTIALLY_FAILED : StatusEnum.SUCCESSFUL;
        this.partiallyFailedInputs = failedNodeIdsInSingleInput;
        this.generalInfo = "";
        
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
            Map<OsmPrimitive, String> failedNodeIdsInSingleInput, Map<String, Object> extraData
    ) {
        this.outputIntents = outputIntents;
        this.representatives = representatives;
        
        this.status = failedNodeIdsInSingleInput != null && !failedNodeIdsInSingleInput.isEmpty() ?
                StatusEnum.PARTIALLY_FAILED : StatusEnum.SUCCESSFUL;
        this.partiallyFailedInputs = failedNodeIdsInSingleInput;
        this.generalInfo = "";
        
        this.extraData = extraData;
    }
    /**
     * 构造函数（单组输入完全失败）
     * @param generalInfo 失败原因
     */
    public ColumbinaSingleOutput(String generalInfo) {
        this.outputIntents = new ArrayList<>();
        this.representatives = new ArrayList<>();
        
        this.status = StatusEnum.FAILED;
        this.partiallyFailedInputs = new HashMap<>();
        this.generalInfo = generalInfo;
        
        this.extraData = new HashMap<>();
    }

    // TODO：可能需要更好的isValid判断
    public boolean isValid() {
        return !(outputIntents == null || outputIntents.isEmpty());
    }
    
    public String concludeFailedInfo() {
        return partiallyFailedInputs.entrySet().stream()
                .collect(Collectors.groupingBy(  // 转为新Map<原因, 要素id列表>
                        Map.Entry::getValue,  // 按原因分组（key），类型是String
                        Collectors.mapping(  // 每组的内容（value），类型是List<OsmPrimitive>
                                Map.Entry::getKey,  // entry -> String.valueOf(entry.getKey().getUniqueId()),
                                Collectors.toList()  // Collectors.joining(", ", "[", "]")
                        )
                ))
                .entrySet().stream()
                .map(entryByReason -> entryByReason.getKey() + ": " + UtilsData.featureListToString(entryByReason.getValue()))
                .collect(Collectors.joining("\n"));  // 拼接不同原因的info
    }
}


