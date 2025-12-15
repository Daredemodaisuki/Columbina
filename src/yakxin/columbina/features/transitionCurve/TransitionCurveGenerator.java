package yakxin.columbina.features.transitionCurve;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsArc;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
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
                    input.ways.getFirst(),
                    params.surfaceRadius, params.surfaceTransArcLength, params.chainageNum
            );
        }
        return null;
    }

    /**
     * 为整条路径生成过渡曲线（缓和曲线）
     * 类似于FilletGenerator.buildSmoothPolyline和ChamferGenerator.buildChamferPolyline
     *
     * @param way 输入的路径
     * @param surfaceRadius 圆曲线地表半径（米）
     * @param surfaceLength 缓和曲线地表长度（米）
     * @param surfaceChainage 桩距（节点间距，米）
     * @return 包含新节点列表和失败节点ID的DrawingNewNodeResult
     */
    public static ColumbinaSingleOutput buildTransitionCurvePolyline(
            Way way,
            double surfaceRadius, double surfaceLength, double surfaceChainage
    ) {
        List<Long> failedNodes = new ArrayList<>();
        // 获取路径的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());  // 获取节点（包含重复的首末点）
        int numNode = way.isClosed() ? way.getNodesCount() - 1 : way.getNodesCount();  // 实际节点数（去除闭合点）
        if (numNode < 3) return null;  // 路径至少需要3个点
        int numCorner = way.isClosed() ? numNode : numNode - 2;

        // 存储每个拐角的过渡曲线结果
        List<List<EastNorth>> transCurves = new ArrayList<>();

        // 为非闭合路径计算所有拐角
        for (int i = 0; i < numCorner; i ++) {
            try {
                // JOSM用Mercator投影的NorthEast坐标等角不等距，需要重算距离，以拐点B取维度计算
                ColumbinaCorner corner = ColumbinaCorner.create(way, i);
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, corner.latB);
                double enLength = UtilsMath.surfaceDistanceToEastNorth(surfaceLength, corner.latB);
                double enChainage = UtilsMath.surfaceDistanceToEastNorth(surfaceChainage, corner.latB);

                // 有EN长度之后继续算圆弧
                ArrayList<EastNorth> transCurve = UtilsArc.getTransCurve(  // 为每个拐角计算缓和曲线
                        corner,
                        enRadius, enLength, enChainage
                );

                if (transCurve == null || transCurve.isEmpty()) {  // 该拐角没有生成缓和曲线
                    transCurves.add(null);
                    failedNodes.add(nodes.get(i + 1).getUniqueId());  // 记录失败拐角
                } else {
                    transCurves.add(transCurve);
                }
            } catch (ColumbinaException | IllegalArgumentException e) {
                // 如果纬度接近90度，使用一个很小的正数，避免除0，但这样不准确，所以直接失败跳过这个圆弧吧
                transCurves.add(null);
                failedNodes.add(nodes.get(i + 1).getUniqueId());
            }
        }
        if (transCurves.isEmpty()) {  // 没有曲线，返回原始路径
            return new ColumbinaSingleOutput(new ArrayList<>(nodes), failedNodes);
        }

        // 最终的经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        // 对于非闭合路径（或闭合点没有曲线的闭合路径），从第一个节点开始；
        // 对于闭合路径且闭合点有曲线，从第一条曲线第一个点开始（下面for中添加）
        if (!way.isClosed() || (way.isClosed() && transCurves.getLast() == null))
            finalNodes.add(nodes.getFirst());
        // 遍历所有拐角
        for (int i = 0; i < numCorner; i ++) {
            if (transCurves.get(i) != null) {  // 有过渡曲线就添加曲线上的所有点
                List<EastNorth> curve = transCurves.get(i);
                for (EastNorth eastNorth : curve)
                    finalNodes.add(new Node(UtilsMath.toLatLon(eastNorth)));
            } else finalNodes.add(nodes.get(i + 1));  // 没有过渡曲线，添加原始拐点
        }
        // 添加最后一个节点
        if (way.isClosed()) finalNodes.add(finalNodes.getFirst());
        else finalNodes.add(nodes.getLast());

        return new ColumbinaSingleOutput(finalNodes, failedNodes);
    }

    /*
    TODO：边缘情况检查
    注意极端几何：R太小、α太小
    检查各种null的情况
     */
}


