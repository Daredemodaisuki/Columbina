package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsData;

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
        // 计算交点（TODO：考虑这段应当挪到action类具体检查时做，并把交点坐标作为快捷传递中间量？）
        // 点startNode(x1, y1) + t·方向向量startDirVec(dx1, dy1) = 点endNode(x2, y2) + s·方向向量endDirVec(dx2, dy2)
        // x1 + t * dx1 = x2 + s * dx2
        // y1 + t * dy1 = y2 + s * dy2
        // 解得 t = [(y₂-y₁)·dx₂ - (x₂-x₁)·dy₂] / (dx₂·dy₁ - dx₁·dy₂)
        int startNodeNum = startWay.isClosed() ? startWay.getNodesCount() - 1 : startWay.getNodesCount();  // 闭合曲线过滤闭合点
        int endNodeNum = endWay.isClosed() ? endWay.getNodesCount() - 1 : endWay.getNodesCount();
        int startNodeIdx = UtilsData.getNodeIndex(startNode, startWay);
        int endNodeIdx = UtilsData.getNodeIndex(endNode, endWay);
        // 点
        ColumbinaEN start = new ColumbinaEN(startNode.getEastNorth()), end = new ColumbinaEN(endNode.getEastNorth());
        // 入曲线和出曲线方向向量
        ColumbinaEN startDirVec = new ColumbinaEN(startWay.getNode((startNodeIdx + startNodeNum - 1) % startNodeNum), startNode).normVec();
        ColumbinaEN endDirVec = new ColumbinaEN(endNode, endWay.getNode((endNodeIdx + 1) % endNodeNum)).normVec();
        double t = (
                (end.north() - start.north()) * endDirVec.east()
                - (end.east() - start.east()) * endDirVec.north()
        ) / (endDirVec.east() * startDirVec.north() - startDirVec.east() * endDirVec.north());
        // 交点
        ColumbinaEN intersect = start.walk(startDirVec.bearingRad(), t);
        // TODO：action类需要检查是不是找不到上下一个节点
        // TODO：需要检查平行（分母=0）
        // TODO：需要检查交点在起点前还是后、终点前还是后

        return null;
    }
}
