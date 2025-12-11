package yakxin.columbina.data.dto.inputs;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;


/**
 * 整个输入由action类分包之后的喂给generator的单个输入
 */
public class ColumbinaSingleInput {
    public List<Node> nodes;
    public List<Way> ways;

    public ColumbinaSingleInput(List<Node> nodes, List<Way> ways) {
        this.nodes = nodes;
        this.ways = ways;
    }

    /**
     * 对于不支持批量操作、无需分包的action，直接调用此函数从总输入直接转为ColumbinaSingleInput
     * @param totalInput 总输入
     */
    public ColumbinaSingleInput(ColumbinaInput totalInput) {
        this.nodes = totalInput.getNodes();
        this.ways = totalInput.getWays();
    }
}
