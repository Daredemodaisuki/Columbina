package yakxin.columbina.features.fillet;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.List;

import static yakxin.columbina.utils.UtilsArc.getCircleArc;


// 圆角计算器
public final class FilletGenerator extends AbstractGenerator<FilletParams> {

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, FilletParams params) {
        if (input.ways != null && input.ways.size() == 1) {
            return buildSmoothPolyline(
                    input.ways.get(0),
                    params.surfaceRadius, params.surfaceChainageLength, params.maxPointNum,
                    params.minAngleDeg, params.maxAngleDeg
            );
        }
        return null;
    }

    /**
     * 绘制一个圆角
     * @param corner 拐角
     * @param enRadius 东北坐标倒角半径
     * @param enChainageLength 桩距（节点间距，米）
     * @param maxNumPoints 最大节点数
     * @param minAngleDeg 最小允许倒角角度
     * @param maxAngleDeg 最大允许倒角角度
     * @return 这个拐角倒圆角包含的点
     */
    private static ArrayList<EastNorth> getFilletArc(
            ColumbinaCorner corner,
            double enRadius, double enChainageLength, int maxNumPoints,
            double minAngleDeg, double maxAngleDeg
    ) {
        double theta = corner.angleRad;  // 拐点张角
        // 检查张角有效性、切距
        if (theta < Math.toRadians(minAngleDeg) || theta > Math.toRadians(maxAngleDeg)) return null;  // 自定义角度控制
        if (theta < UtilsMath.EPSILON_STRICT || theta > Math.PI - UtilsMath.EPSILON_STRICT) return null;  // θ为0说明成了发卡角，θ为π说明张角基本是直线
        double tangentLength = enRadius / Math.tan(theta / 2.0);
        if (corner.lenBA < tangentLength || corner.lenBC < tangentLength) return null;  // 切距不足
        // 节点数控制
        int arcSegments;  // 曲线有多少桩段，也就是不计首尾的节点数+1
        if (enChainageLength * (maxNumPoints + 1) < (Math.PI - theta) * enRadius)  // 如果最大段数*点距不足弧线长度（π-θ是圆心角或路径偏转角）
            arcSegments = maxNumPoints + 1;  // 强制使用最大段数，会扩大点距离
        else arcSegments = Math.max(1, Math.min(  // max保证至少有1点，min防止ceil成maxNumPoints+2了多一个点
                maxNumPoints + 1,
                (int) (Math.ceil((Math.PI - theta) * enRadius) / enChainageLength)
        ));
        // 圆心
        double centerToB = enRadius / Math.sin(theta / 2.0);  // B到圆心的距离
        ColumbinaEN center = corner.B.walk(corner.getBisectorBearingRad(), centerToB);

        // 计算、返回
        return (ArrayList<EastNorth>)
                getCircleArc(
                        center, enRadius,
                        corner.AB.bearingRad(), corner.BC.bearingRad(),
                        arcSegments,
                        corner.leftRight
                );
    }

    public static ColumbinaSingleOutput buildSmoothPolyline(Way way, double surfaceRadius) {
        return buildSmoothPolyline(way, surfaceRadius, 20);
    }
    public static ColumbinaSingleOutput buildSmoothPolyline(Way way, double surfaceRadius, int maxPointNum) {
        return buildSmoothPolyline(way, surfaceRadius, 1.0, maxPointNum, UtilsMath.EPSILON_STRICT, 180 - UtilsMath.EPSILON_STRICT);
    }

    /**
     * 为整条路径倒圆角
     * @param way 输入的路径
     * @param surfaceRadius 圆曲线地表半径（米）
     * @param surfaceChainageLength 桩距（节点间距，米）
     * @param maxPointNum 最大节点数
     * @param minAngleDeg 最小允许倒角角度
     * @param maxAngleDeg 最大允许倒角角度
     * @return 包含新节点列表和失败节点ID的ColumbinaSingleOutput
     */
    public static ColumbinaSingleOutput buildSmoothPolyline(
            Way way,
            double surfaceRadius, double surfaceChainageLength, int maxPointNum,
            double minAngleDeg, double maxAngleDeg
    ) {
        List<Long> failedNodes = new ArrayList<>();
        // 获取路径的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());
        int numNode = way.isClosed() ? way.getNodesCount() - 1 : way.getNodesCount();  // 实际节点数（去除闭合点）
        if (numNode < 3) return null;  // 路径至少需要3个点
        int numCorner = way.isClosed() ? numNode : numNode - 2;

        // 存储每个拐角的过渡曲线结果
        List<List<EastNorth>> filletCurves = new ArrayList<>();

        // 为路径计算所有拐角
        for (int i = 0; i < numCorner; i ++) {
            try {
                ColumbinaCorner corner = ColumbinaCorner.create(way, i);
                double latB = corner.latB;
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, latB);
                double enChainageLength = UtilsMath.surfaceDistanceToEastNorth(surfaceChainageLength, latB);
                // 有EN长度之后继续算圆弧
                List<EastNorth> arc = getFilletArc(  // 为每个拐角生成PNum个点的圆弧
                        corner,
                        enRadius, enChainageLength, maxPointNum,
                        minAngleDeg, maxAngleDeg
                );
                if (arc == null || arc.size() < 2) {  // 该拐角没有生成圆角（半径过大或角度问题）
                    filletCurves.add(null);
                    failedNodes.add(nodes.get(i + 1).getUniqueId());
                }
                else {
                    filletCurves.add(arc);
                }
            } catch (ColumbinaException exSurToEN) {
                // 如果纬度接近90度，使用一个很小的正数，避免除0，但这样不准确，所以直接失败跳过这个圆弧吧
                filletCurves.add(null);
                failedNodes.add(nodes.get(i + 1).getUniqueId());
            }
        }

        // 最终的节点经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        // 对于非闭合路径（或闭合点没有曲线的闭合路径），从第一个节点开始；
        // 对于闭合路径且闭合点有曲线，从第一条曲线第一个点开始（下面for中添加）
        if (!way.isClosed() || (way.isClosed() && filletCurves.get(filletCurves.size() - 1) == null))
            finalNodes.add(nodes.get(0));
        // 遍历所有拐角
        for (int i = 0; i < numCorner; i ++) {
            if (filletCurves.get(i) != null) {  // 有过渡曲线就添加曲线上的所有点
                List<EastNorth> curve = filletCurves.get(i);
                for (EastNorth eastNorth : curve)
                    finalNodes.add(new Node(UtilsMath.toLatLon(eastNorth)));
            } else finalNodes.add(nodes.get(i + 1));  // 没有过渡曲线，添加原始拐点
        }
        // 添加最后一个节点
        if (way.isClosed()) finalNodes.add(finalNodes.get(0));
        else finalNodes.add(nodes.get(nodes.size() - 1));

        return new ColumbinaSingleOutput(finalNodes, failedNodes);
        // 正式绘制前注意去重
    }
}


