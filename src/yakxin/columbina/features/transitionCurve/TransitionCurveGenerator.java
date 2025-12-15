package yakxin.columbina.features.transitionCurve;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaEN;
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
        List<Node> nodes = new ArrayList<>(way.getNodes());
        int nPts = nodes.size();
        if (nPts < 3) return null;  // 路径至少需要3个点

        // 将所有节点转换为平面坐标
        List<EastNorth> nodesEN = new ArrayList<>();
        for (Node n : nodes) nodesEN.add(UtilsMath.toEastNorth(n.getCoor()));

        // 存储每个拐角的过渡曲线结果
        List<List<EastNorth>> transCurves = new ArrayList<>();

        // 为非闭合路径计算所有拐角
        for (int i = 0; i < nPts - 2; i++) {
            EastNorth A = nodesEN.get(i);      // 起点    B → C
            EastNorth B = nodesEN.get(i + 1);  // 拐点    ↑
            EastNorth C = nodesEN.get(i + 2);  // 终点    A
            ColumbinaCorner corner = ColumbinaCorner.create(way, i);
            // TODO：将输入之间换为corner，for改一下不要后面单独if (way.isClosed())
            //  改成i<拐角数-1，拐角数=way.isClosed()?去重后的实际节点数-2（nPts-2）:去重后的实际节点数（nPts-1，nPts没去重，所以-）

            try {
                // JOSM用Mercator投影的NorthEast坐标等角不等距，需要重算距离，以拐点B取维度计算
                double latB = nodes.get(i + 1).getCoor().lat();
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, latB);
                double enLength = UtilsMath.surfaceDistanceToEastNorth(surfaceLength, latB);
                double enChainage = UtilsMath.surfaceDistanceToEastNorth(surfaceChainage, latB);
                // 有EN长度之后继续算圆弧
                ArrayList<EastNorth> transCurve = UtilsArc.getTransCurve(  // 为每个拐角计算缓和曲线
                        new ColumbinaCorner(A, B, C),
                        enRadius, enLength, enChainage
                );

                if (transCurve == null || transCurve.isEmpty()) {  // 该拐角没有生成缓和曲线
                    transCurves.add(null);
                    failedNodes.add(nodes.get(i + 1).getUniqueId());  // 添加拐角
                } else {
                    transCurves.add(transCurve);
                }
            } catch (ColumbinaException | IllegalArgumentException e) {
                transCurves.add(null);
                failedNodes.add(nodes.get(i + 1).getUniqueId());
            }
        }
        if (way.isClosed()) {  // 闭合曲线首尾相连的首末点曲线
            try {
                double latB = nodes.getFirst().getCoor().lat();
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, latB);
                double enLength = UtilsMath.surfaceDistanceToEastNorth(surfaceLength, latB);
                double enChainage = UtilsMath.surfaceDistanceToEastNorth(surfaceChainage, latB);

                ArrayList<EastNorth> transCurve = UtilsArc.getTransCurve(
                        new ColumbinaCorner(nodesEN.get(nPts - 2), nodesEN.get(0), nodesEN.get(1)),  // nodesEN.get(0) = getFirst() == nodesEN.get(-1)
                        enRadius, enLength, enChainage
                );

                if (transCurve == null || transCurve.isEmpty()) {
                    transCurves.add(null);
                    failedNodes.add(nodes.getFirst().getUniqueId());
                } else {
                    transCurves.add(transCurve);
                }
            } catch (ColumbinaException | IllegalArgumentException e) {
                transCurves.add(null);
                failedNodes.add(nodes.getFirst().getUniqueId());
            }
        }
        if (transCurves.isEmpty()) {  // 没有曲线，返回原始路径
            return new ColumbinaSingleOutput(new ArrayList<>(nodes), failedNodes);
        }

        // 最终的经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        if (!way.isClosed()) {  // 对于非闭合路径，从第一个节点开始
            finalNodes.add(nodes.getFirst());
            // 遍历所有拐角
            for (int i = 0; i < nPts - 2; i++) {
                if (transCurves.get(i) != null) {  // 有过渡曲线就添加曲线上的所有点
                    List<EastNorth> curve = transCurves.get(i);
                    for (int j = 0; j < curve.size(); j++) {
                        finalNodes.add(new Node(UtilsMath.toLatLon(curve.get(j))));
                    }
                } else finalNodes.add(nodes.get(i + 1));  // 没有过渡曲线，添加原始拐点
            }
            // 添加最后一个节点
            finalNodes.add(nodes.getLast());
        } else {  // 闭合路径处理
            // 检查最后一个拐角（闭合点）是否有曲线
            List<EastNorth> lastCurve = transCurves.getLast();
            if (lastCurve != null && !lastCurve.isEmpty()) {  // 有则使用最后一条曲线（闭合点）的最后一个点作为起点
                EastNorth startEN = lastCurve.getLast();
                LatLon startLL = UtilsMath.toLatLon(startEN);
                finalNodes.add(new Node(startLL));
            } else finalNodes.add(nodes.getFirst());  // 否则使用原始起点
            // 遍历所有拐角（包括闭合点那个）
            // nPts点的路径（索引0~nPts-1），去除重复的首尾实际只有nPts-1个点（索引0~nPts-2）
            // 曲线一共nPts-1条，则索引nPts-3为除了闭合点那条曲线的最后一条，nPts-2为闭合点那条
            // 对于闭合点那条，如果无法生成，则使用闭合点
            for (int i = 0; i < nPts - 1; i++) {
                if (i < transCurves.size() && transCurves.get(i) != null) {  // 有过渡曲线就添加曲线上的所有点
                    List<EastNorth> curve = transCurves.get(i);
                    for (int j = 0; j < curve.size(); j++) {
                        finalNodes.add(new Node(UtilsMath.toLatLon(curve.get(j))));
                    }
                } else finalNodes.add(nodes.get(i + 1));
            }
        }

        return new ColumbinaSingleOutput(finalNodes, failedNodes);
    }

    /*
    TODO：边缘情况检查
    注意极端几何：R太小、α太小
    检查各种null的情况
     */
}


