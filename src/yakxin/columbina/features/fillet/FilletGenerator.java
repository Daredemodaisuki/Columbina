package yakxin.columbina.features.fillet;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.DrawingNewNodeResult;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.List;


// 圆角计算器
public final class FilletGenerator extends AbstractGenerator<FilletParams> {

    @Override
    public DrawingNewNodeResult getNewNodeWayForSingleInput(Object input, FilletParams params) {
        if (input instanceof Way) {
            return buildSmoothPolyline(
                    (Way) input,
                    params.surfaceRadius, params.surfaceChainageLength, params.maxPointNum,
                    params.minAngleDeg, params.maxAngleDeg
            );
        }
        return null;
    }

    /// 圆角算法
    // 绘制一个圆角（需要输入重算距离而不是直接的地表距离）
    private static ArrayList<EastNorth> getFilletArc(
            EastNorth A, EastNorth B, EastNorth C,
            double enRadius, double enChainageLength, int maxNumPoints,
            double minAngleDeg, double maxAngleDeg
    ) {
        // 获取node坐标
        double[] a = new double[]{A.getX(), A.getY()};  // A起点     B → C
        double[] b = new double[]{B.getX(), B.getY()};  // B拐点     ↑
        double[] c = new double[]{C.getX(), C.getY()};  // C终点     A
//
        // // 方向向量
        // double[] v1 = UtilsMath.sub(a, b);  // BA向量
        // double[] v2 = UtilsMath.sub(c, b);  // BC向量
        // double n1 = UtilsMath.norm(v1);     // BA长度
        // double n2 = UtilsMath.norm(v2);     // BC长度
        // if (n1 < 1e-9 || n2 < 1e-9) return null;  // 检查向量有效性（不能太短近乎于点挤在一起）
//
        // double[] u1 = UtilsMath.mul(v1, 1.0 / n1);  // BA单位向量
        // double[] u2 = UtilsMath.mul(v2, 1.0 / n2);  // BC单位向量
//
        // // 拐点夹角（方向向量点积取acos）
        // double dp = Math.max(-1.0, Math.min(1.0, UtilsMath.dot(u1, u2)));  // 点积（限制在[-1,1]范围内）
        // double theta = Math.acos(dp);  // 夹角（弧度[0,π]）
//
        // // 检查张角有效性
        // double minAngleRad = Math.toRadians(minAngleDeg);
        // double maxAngleRad = Math.toRadians(maxAngleDeg);
        // if (theta < minAngleRad || theta > maxAngleRad) return null;  // 自定义角度控制
        // if (theta < 1e-9 || theta > Math.PI - 1e-9) return null;  // θ为0说明成了发卡角，θ为π说明张角基本是直线
//
        // // 圆弧的起点和终点（切点）
        // double t = enRadius / Math.tan(theta / 2.0);  // 从B到两侧切点的距离
        // if (t > n1 - 1e-9 || t > n2 - 1e-9) return null;  // 检查半径是否过大
        // double[] T1 = UtilsMath.add(b, UtilsMath.mul(u1, t));  // BA上的切点坐标T1
        // double[] T2 = UtilsMath.add(b, UtilsMath.mul(u2, t));  // BC上的切点坐标T2
//
        // // 圆弧圆心坐标
        // double d = enRadius / Math.sin(theta / 2.0);  // B到圆心的距离
        // double[] bis = UtilsMath.add(u1, u2);  // 角平分线方向
        // if (UtilsMath.norm(bis) < 1e-12) return null;  // 角平分线是否有效
        // double[] center = UtilsMath.add(b, UtilsMath.mul(bis, d / UtilsMath.norm(bis))); // 圆心坐标
//
        // // 圆弧起始和结束角度
        // double ang1 = UtilsMath.getBearingRadFromAtoB(center, T1);  // 圆心到T1的角度
        // double ang2 = UtilsMath.getBearingRadFromAtoB(center, T2);  // 圆心到T2的角度

        // 方向向量
        double[] BA = UtilsMath.sub(a, b);
        double[] BC = UtilsMath.sub(c, b);
        double[] AB = UtilsMath.mul(BA, -1.0);
        // 拐点张角（方向向量点积取acos）和张角有效性
        double theta = UtilsMath.getAngleRadBetweenVec(BA, BC);
        if (theta < Math.toRadians(minAngleDeg) || theta > Math.toRadians(maxAngleDeg)) return null;  // 自定义角度控制
        if (theta < 1e-9 || theta > Math.PI - 1e-9) return null;  // θ为0说明成了发卡角，θ为π说明张角基本是直线
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
        double[] center = UtilsMath.walkAlongAngleDistance(  // 沿着ABC角平分线方向走B到圆心的距离
                b, UtilsMath.getVecBearingRad(UtilsMath.add(
                        UtilsMath.getUnitVec(BA),
                        UtilsMath.getUnitVec(BC)
                )), centerToB
        );
        EastNorth enCenter = new EastNorth(center[0], center[1]);
        // 进出曲线方向角
        double startBearingRad = UtilsMath.getVecBearingRad(AB);
        double endBearingRad = UtilsMath.getVecBearingRad(BC);
        int leftRight = UtilsMath.getLeftRight(AB, BC);

        // 计算、返回
        return (ArrayList<EastNorth>)
                UtilsMath.getCircleArcPointsWithBearingRad(
                        enCenter, enRadius,
                        startBearingRad, endBearingRad,
                        arcSegments,
                        leftRight
                );

        // // 圆弧方向
        // double crossz = u1[0] * u2[1] - u1[1] * u2[0];  // 向量叉积的Z分量
        // if (crossz < 0) {
        //     // 逆时针方向（BA到BC需要逆时针旋转），确保ang2 > ang1
        //     if (ang2 < ang1) ang2 += 2*Math.PI;
        // } else {
        //     // 顺时针方向，确保ang2 < ang1
        //     if (ang2 > ang1) ang2 -= 2*Math.PI;
        // }
//
        // // 生成圆弧上的点
        // int numAngleSteps = Math.max(  // 步进段数，不算开头结尾的点数=步进段数-2
        //         Math.min((int) ((Math.PI - theta) / Math.toRadians(enChainageLength)), maxNumPoints + 1),  // 最大50点的话步进有51段
        //         1  // 注意θ是张角，张角越小圆心角越大、弧长越长，点数越多
        // );  // 至少1段0点（相当于切角），至多maxNumPoints+1段maxNumPoints点
        // // TODO：切角可以快速实现了
        // ArrayList<EastNorth> arc = new ArrayList<>();
        // for (int i = 0; i <= numAngleSteps; i ++){
        //     double tt = (double) i / numAngleSteps;    // 插值参数 [0,1]
        //     double ang = ang1 + (ang2 - ang1) * tt;    // 当前角度
        //     double x = center[0] + enRadius * Math.cos(ang);  // 圆弧点X坐标
        //     double y = center[1] + enRadius * Math.sin(ang);  // 圆弧点Y坐标
        //     arc.add(new EastNorth(x, y));
        // }
        // return arc;
    }

    public static DrawingNewNodeResult buildSmoothPolyline(Way way, double surfaceRadius) {
        return buildSmoothPolyline(way, surfaceRadius, 20);
    }
    public static DrawingNewNodeResult buildSmoothPolyline(Way way, double surfaceRadius, int maxPointNum) {
        return buildSmoothPolyline(way, surfaceRadius, 1.0, maxPointNum, 1e-9, 180 - 1e-9);
    }
    // 汇总所有的圆角
    public static DrawingNewNodeResult buildSmoothPolyline(
            Way way,
            double surfaceRadius, double surfaceChainageLength, int maxPointNum,
            double minAngleDeg, double maxAngleDeg
    ) {
        List<Long> failedNodes = new ArrayList<>();
        // 获取路径的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());
        int nPts = nodes.size();
        if (nPts < 3) return null;  // 路径至少需要3个点

        // 将所有节点转换为平面坐标
        List<EastNorth> nodesEN = new ArrayList<>();
        for (Node n : nodes) nodesEN.add(UtilsMath.toEastNorth(n.getCoor()));

        // 为每个拐角预计算圆角
        List<double[]> T1s = new ArrayList<>();  // 存储每个拐角的第一个切点
        List<List<EastNorth>> arcs = new ArrayList<>();  // 存储每个拐角的圆弧
        for (int i = 0; i < nPts - 2; i ++) {
            EastNorth A = nodesEN.get(i);      // 起点    B → C
            EastNorth B = nodesEN.get(i + 1);  // 拐点    ↑
            EastNorth C = nodesEN.get(i + 2);  // 终点    A

            // JOSM用Mercator投影的NorthEast坐标等角不等距，需要重算距离，以拐点B取维度计算
            try {
                double latB = nodes.get(i + 1).getCoor().lat();
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, latB);
                double enChainageLength = UtilsMath.surfaceDistanceToEastNorth(surfaceChainageLength, latB);
                // 有EN长度之后继续算圆弧
                List<EastNorth> arc = getFilletArc(  // 为每个拐角生成PNum个点的圆弧
                        A, B, C,
                        enRadius, enChainageLength, maxPointNum,
                        minAngleDeg, maxAngleDeg
                );

                if (arc == null) {  // 该拐角没有生成圆角（半径过大或角度问题）
                    T1s.add(null);
                    arcs.add(null);
                    failedNodes.add(nodes.get(i).getUniqueId());
                } else {  // 存储切点和圆弧
                    EastNorth t1 = arc.getFirst();
                    T1s.add(new double[]{t1.getX(), t1.getY()});
                    arcs.add(arc);
                }
            } catch (ColumbinaException exSurToEN) {
                // 如果纬度接近90度，使用一个很小的正数，避免除0，但这样不准确，所以直接失败跳过这个圆弧吧
                T1s.add(null);
                arcs.add(null);
                failedNodes.add(nodes.get(i).getUniqueId());
            }
        }
        if (way.isClosed()) {  // 闭合曲线首尾相连的首末点曲线
            try {
                double latB = nodes.getFirst().getCoor().lat();
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, latB);
                double enChainageLength = UtilsMath.surfaceDistanceToEastNorth(surfaceChainageLength, latB);
                List<EastNorth> arcEnd = getFilletArc(
                        nodesEN.get(nPts - 2), nodesEN.get(0), nodesEN.get(1),  // nodesEN.get(0) = getFirst() == nodesEN.get(-1)
                        enRadius, enChainageLength, maxPointNum,
                        minAngleDeg, maxAngleDeg
                );
                if (arcEnd == null) {
                    T1s.add(null);
                    arcs.add(null);
                    failedNodes.add(nodes.getFirst().getUniqueId());
                } else {
                    EastNorth t1End = arcEnd.getFirst();
                    T1s.add(new double[]{t1End.getX(), t1End.getY()});
                    arcs.add(arcEnd);
                }
            } catch (ColumbinaException exSurToEN) {
                T1s.add(null);
                arcs.add(null);
                failedNodes.add(nodes.getFirst().getUniqueId());
            }
        }

        // 最终的经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        // 添加起始点：如果原路径闭合，且首末点有曲线，则以首末点曲线（arcs最后一个）终点为整个新路径起点；否则使用原路径第一个节点
        boolean useLastArcLastNode = way.isClosed() && arcs.getLast() != null;
        if (useLastArcLastNode) finalNodes.add(new Node(UtilsMath.toLatLon(arcs.getLast().getLast())));
        else finalNodes.add(way.getNode(0));
        // 遍历除最后一个节点以外所有路径节点，用圆弧替换拐角
        for (int i = 0; i < nPts - 2; i ++) {
            if (arcs.get(i) != null) {  // 检查本次的拐角B（i+1）是否有有效圆角（圆角编号=A编号=i），圆弧存在则使用圆角路径
                double[] T1 = T1s.get(i);
                // 添加圆弧上第一个切点（如果与上个点不同）
                Node curveFirst = new Node(UtilsMath.toLatLon(new EastNorth(T1[0], T1[1])));
                if (!finalNodes.getLast().equals(curveFirst)) finalNodes.add(curveFirst);
                // 添加圆弧上其余点（跳过第一个点，避免重复）
                List<EastNorth> arc = arcs.get(i);
                for (int k = 1; k < arc.size(); k ++)
                    finalNodes.add(new Node(UtilsMath.toLatLon(arc.get(k))));
            } else {  // 圆弧不存在则直接将拐点B加进来
                finalNodes.add(way.getNode(i + 1));
            }
        }
        // 终点处理
        if (way.isClosed()) {
            if (arcs.getLast() != null) {  // 如果原路径闭合，且首末点有曲线，拼上最后一条曲线并连上起点
                List<EastNorth> arcClosedEnd = arcs.getLast();  // 对于闭合路径nPts-2倒数第2个点，nPts-1最后一个点=0起点，1表示第2个点
                for (int k = 0; k < arcClosedEnd.size(); k ++)
                    finalNodes.add(new Node(UtilsMath.toLatLon(arcClosedEnd.get(k))));
                finalNodes.add(finalNodes.getFirst());
            } else finalNodes.add(way.getNode(0));
        } else {
            finalNodes.add(way.getNode(way.getNodesCount() - 1));
        }

        return new DrawingNewNodeResult(finalNodes, failedNodes);
        // 正式绘制前注意去重
    }
}


