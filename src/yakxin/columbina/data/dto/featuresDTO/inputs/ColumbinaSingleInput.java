package yakxin.columbina.data.dto.featuresDTO.inputs;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 整个输入由action类分包之后的喂给generator的单个输入
 */
public final class ColumbinaSingleInput {
    public final List<Node> nodes;
    public final List<Way> ways;

    // 快捷传递中间量：如果在检查期间就预计算了一些内容（比如路径上的节点索引），可以赋值扔这里方便的给到生成器减少重复计算，生成器需要自己拆包
    public Map<String, Object> quickPrecomputedData;

    /**
     * 构建单组输入
     * @param nodes 输入节点列表
     * @param ways 输入路径列表
     */
    public ColumbinaSingleInput(List<Node> nodes, List<Way> ways) {
        this(nodes, ways, new HashMap<>());
    }

    /**
     * 带快捷传递中间量的构建单组输入
     * @param nodes 输入节点列表
     * @param ways 输入路径列表
     * @param quickPrecomputedData 快捷传递中间量列表
     */
    public ColumbinaSingleInput(List<Node> nodes, List<Way> ways, Map<String, Object> quickPrecomputedData) {
        this.nodes = nodes;
        this.ways = ways;
        this.quickPrecomputedData = quickPrecomputedData;
    }

    /**
     * 对于不支持批量操作、无需分包的action，直接调用此函数从总输入直接转为ColumbinaSingleInput
     * @param totalInput 总输入
     */
    public ColumbinaSingleInput(ColumbinaInput totalInput) {
        this(totalInput, new HashMap<>());
    }

    /**
     * 对于不支持批量操作、无需分包的action，直接调用此函数从总输入直接转为ColumbinaSingleInput，并同时传入快捷传递中间量
     * @param totalInput 总输入
     * @param quickPrecomputedData 快捷传递中间量列表
     */
    public ColumbinaSingleInput(ColumbinaInput totalInput, Map<String, Object> quickPrecomputedData) {
        this.nodes = totalInput.getNodes();
        this.ways = totalInput.getWays();
        this.quickPrecomputedData = quickPrecomputedData;
    }
    
    public List<OsmPrimitive> getMixedInputList() {
        List<OsmPrimitive> result = new ArrayList<>();
        result.addAll(nodes);
        result.addAll(ways);
        return result;
    }
}


