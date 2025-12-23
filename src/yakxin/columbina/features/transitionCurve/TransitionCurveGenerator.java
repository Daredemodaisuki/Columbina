package yakxin.columbina.features.transitionCurve;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.outputs.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsArc;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**缓和曲线计算类
 * <p>本文件角度定义:
 * <ul>
 *     <li>坐标角度：以东为0，（逆时针）北正（顺时针）南负，区间在[-pi, pi]，±pi等同处理</li>
 *     <li>用户角度：以路径行进方向为0°，（逆时针）左正（顺时针）右负，区间在[-180°，180°]，±180°等同处理</li>
 * </ul>
 */
public final class TransitionCurveGenerator extends AbstractGenerator<TransitionCurveParams> {

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, TransitionCurveParams params) {
        if (input.ways != null && input.ways.size() == 1) {
            return buildTransitionCurvePolyline(
                    input.ways.get(0),
                    params.surfaceRadius, params.surfaceTransArcLength, params.surfaceChainageLength
            );
        }
        return null;
    }

    /**
     * 绘制一个拐角的完整的缓和曲线（汇总3段子曲线，返回null表示失败）
     * @param corner 拐角
     * @param enCurveRadius 圆曲线半径（R）
     * @param enTransArcLength 缓和曲线长度（ls）
     * @param enChainageLength 桩距（节点间距，米）
     * @return 这个拐角缓和曲线包含的点
     */
    private static ArrayList<EastNorth> getTransCurve(
            ColumbinaCorner corner,
            double enCurveRadius, double enTransArcLength,
            double enChainageLength
    ) {
        // 确定两段双螺旋曲线的起点
        UtilsArc.TransArcStartResult transArcStarts = UtilsArc.getStartsOfEulerArcs(corner, UtilsArc.DIRECT, enCurveRadius, enTransArcLength);
        // 缓和曲线长度太长导致切距超过拐角两侧距离
        if (transArcStarts.enTangentLength > corner.lenBA || transArcStarts.enTangentLength > corner.lenBC)
            return null;
        // TODO：缓和曲线长度太长导致回旋线部分的转角就大于了总偏转角，导致曲线直接绕了一圈

        /// A侧螺旋线（从A侧直缓切点顺着画）
        // 绘制
        UtilsArc.SingleEulerArcResult unrotatedTransArcA = UtilsArc.getUnrotatedEulerArc(
                enCurveRadius, enTransArcLength,
                enChainageLength,
                transArcStarts.leftRight
        );
        // 旋转、移动
        UtilsArc.SingleEulerArcResult rotatedTransArcA = UtilsArc.rotateAndMoveEulerArc(
                transArcStarts.startA,
                transArcStarts.startAngleARad,
                unrotatedTransArcA
        );
        /// C侧螺旋线（从C侧直缓切点开始倒着画）
        // 绘制
        UtilsArc.SingleEulerArcResult unrotatedTransArcC = UtilsArc.getUnrotatedEulerArc(
                enCurveRadius, enTransArcLength,
                enChainageLength,
                -transArcStarts.leftRight  // C侧是倒回来画的，与A到C方向的左右相反
        );
        // 旋转、移动
        UtilsArc.SingleEulerArcResult rotatedTransArcC = UtilsArc.rotateAndMoveEulerArc(
                transArcStarts.startC,
                transArcStarts.startAngleCRad,
                unrotatedTransArcC
        );
        /// 圆曲线
        // 计算圆心（从B向角平分线方向走(圆曲线半径R + 内移距离p) / sin(张角θ / 2)这个长度）
        double enCenterToB = (enCurveRadius + transArcStarts.enShiftDistance) / Math.sin(corner.angleRad / 2);
        ColumbinaEN center = corner.B.walk(corner.getBisectorBearingRad(), enCenterToB);
        // 计算段数（圆心角）
        double tangentBearingARad = rotatedTransArcA.endTangentAngleRad;
        double tangentBearingCRad = UtilsMath.normAngleRad(rotatedTransArcC.endTangentAngleRad + Math.PI);  // C侧双螺旋是倒过来画的，所以它绘制意义上的（终点）出曲线方向取反向才是行进方向A→B→C的角度
        double centralAngleRad = UtilsMath.normAngleRad(tangentBearingCRad - tangentBearingARad);  // 防止AB、BC跨±180°线时画优弧（「<」这种情况）
        int numAngleSteps = Math.abs((int) (enCurveRadius * centralAngleRad / enChainageLength));
        // 画曲线
        List<EastNorth> circularArc = UtilsArc.getCircleArc(
                center, enCurveRadius,
                tangentBearingARad, tangentBearingCRad,
                numAngleSteps, transArcStarts.leftRight
        );
        /// 拼接
        // 整理用于拼接的点
        List<EastNorth> transArcA = new ArrayList<>(rotatedTransArcA.arcNodes);
        List<EastNorth> transArcC = new ArrayList<>(rotatedTransArcC.arcNodes);
        if (transArcA.size() < 2 || circularArc.size() < 2 || transArcC.size() < 2) return null;  // 曲线不完整，绘制失败
        transArcA.remove(transArcA.size() - 1);  // 不要ArcA的最后一个点（=圆曲线第一个点）
        Collections.reverse(transArcC);  // 倒着画的原地逆序正回来
        transArcC.remove(0);  // 正序之后不要第一个点（=圆曲线最后一个点）

        ArrayList<EastNorth> finalNodes = new ArrayList<>();
        finalNodes.addAll(transArcA);
        finalNodes.addAll(circularArc);
        finalNodes.addAll(transArcC);

        return finalNodes;
    }

    /**
     * 为整条路径生成过渡曲线（缓和曲线）
     * 类似于FilletGenerator.buildSmoothPolyline和ChamferGenerator.buildChamferPolyline
     *
     * @param way 输入的路径
     * @param surfaceRadius 圆曲线地表半径（米）
     * @param surfaceLength 缓和曲线地表长度（米）
     * @param surfaceChainageLength 桩距（节点间距，米）
     * @return 包含新节点列表和失败节点ID的ColumbinaSingleOutput
     */
    public static ColumbinaSingleOutput buildTransitionCurvePolyline(
            Way way,
            double surfaceRadius, double surfaceLength, double surfaceChainageLength
    ) {
        List<OsmPrimitive> failedNodes = new ArrayList<>();
        // 获取路径的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());  // 获取节点（包含重复的首末点）
        int numNode = way.isClosed() ? way.getNodesCount() - 1 : way.getNodesCount();  // 实际节点数（去除闭合点）
        if (numNode < 3) return null;  // 路径至少需要3个点
        int numCorner = way.isClosed() ? numNode : numNode - 2;

        // 存储每个拐角的过渡曲线结果
        List<List<EastNorth>> transCurves = new ArrayList<>();

        // 为路径计算所有拐角
        for (int i = 0; i < numCorner; i ++) {
            try {
                // JOSM用Mercator投影的NorthEast坐标等角不等距，需要重算距离，以拐点B取维度计算
                ColumbinaCorner corner = ColumbinaCorner.create(way, i);
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, corner.latB);
                double enLength = UtilsMath.surfaceDistanceToEastNorth(surfaceLength, corner.latB);
                double enChainageLength = UtilsMath.surfaceDistanceToEastNorth(surfaceChainageLength, corner.latB);

                // 有EN长度之后继续算圆弧
                ArrayList<EastNorth> transCurve = getTransCurve(  // 为每个拐角计算缓和曲线
                        corner,
                        enRadius, enLength, enChainageLength
                );

                if (transCurve == null || transCurve.size() < 2) {  // 该拐角没有生成缓和曲线
                    transCurves.add(null);
                    failedNodes.add(nodes.get(i + 1));  // 记录失败拐角
                } else {
                    transCurves.add(transCurve);
                }
            } catch (ColumbinaException | IllegalArgumentException e) {
                // 如果纬度接近90度，使用一个很小的正数，避免除0，但这样不准确，所以直接失败跳过这个圆弧吧
                transCurves.add(null);
                failedNodes.add(nodes.get(i + 1));
            }
        }
        if (transCurves.isEmpty()) {  // 没有曲线，返回原始路径
            return new ColumbinaSingleOutput(new ArrayList<>(nodes), failedNodes);
        }

        // 最终的节点经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        // 对于非闭合路径（或闭合点没有曲线的闭合路径），从第一个节点开始；
        // 对于闭合路径且闭合点有曲线，从第一条曲线第一个点开始（下面for中添加）
        if (!way.isClosed() || (way.isClosed() && transCurves.get(transCurves.size() - 1) == null))
            finalNodes.add(nodes.get(0));
        // 遍历所有拐角
        for (int i = 0; i < numCorner; i ++) {
            if (transCurves.get(i) != null) {  // 有过渡曲线就添加曲线上的所有点
                List<EastNorth> curve = transCurves.get(i);
                for (EastNorth eastNorth : curve)
                    finalNodes.add(new Node(UtilsMath.toLatLon(eastNorth)));
            } else finalNodes.add(nodes.get(i + 1));  // 没有过渡曲线，添加原始拐点
        }
        // 添加最后一个节点
        if (way.isClosed()) finalNodes.add(finalNodes.get(0));
        else finalNodes.add(nodes.get(nodes.size() - 1));

        return new ColumbinaSingleOutput(finalNodes, failedNodes);
    }

    /*
    TODO：边缘情况检查
    注意极端几何：R太小、α太小
    检查各种null的情况
     */
}


