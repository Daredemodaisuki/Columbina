package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

public class CurveConnectGenerator extends AbstractGenerator<CurveConnectParams> {
    // 模式常量，注意这个不是用来判断±1的！
    public static final int COUNTER_CLOCKWISE_MODE = 0;  // 逆时针左拐
    public static final int CLOCKWISE_MODE = 1;  // 顺时针右拐

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, CurveConnectParams params) {
        return null;
    }

    /**
     * 连接输入的两端，形成圆滑拐角
     * @param startNode 起点
     * @param startWay 起点所在路径
     * @param endNode 终点
     * @param endWay 终点所在路径
     * @param enCurveRadius 圆曲线半径（R）
     * @param enTransArcLength 缓和曲线长度（ls）
     * @param enChainageLength 桩距（节点间距，米）
     * @param mode 模式索引
     * @return 这个拐角缓和曲线包含的点
     */
    private static ColumbinaSingleOutput buildCorner(
            Node startNode, Way startWay,
            Node endNode, Way endWay,
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength,  // 每个桩（节点）之间的距离
            int mode
    ) {
        // 先计算交点

        return null;
    }
}
