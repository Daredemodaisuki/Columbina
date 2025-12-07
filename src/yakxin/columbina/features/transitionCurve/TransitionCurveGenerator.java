package yakxin.columbina.features.transitionCurve;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.DrawingNewNodeResult;
import yakxin.columbina.utils.UtilsData;
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


public class TransitionCurveGenerator implements UtilsData.WayGenerator {
    private final double surfaceRadius;
    private final double surfaceLength;
    private final double surfaceChainage;

    public static final int LEFT = 1;
    public static final int RIGHT = -LEFT;  // -1
    public static final int TERM_MAX = 10;  // 前11项（n从0到10）

    public TransitionCurveGenerator(double surfaceRadius, double surfaceLength, double surfaceChainage) {
        this.surfaceRadius = surfaceRadius;
        this.surfaceLength = surfaceLength;
        this.surfaceChainage = surfaceChainage;
    }

    @Override
    public DrawingNewNodeResult getNewNodeWay(Way way) {
        return buildTransitionCurvePolyline(
                way,
                surfaceRadius, surfaceLength, surfaceChainage
        );
    }

    /// 求和级数的单项：使用匿名函数具体定义TermFunction的抽象compute方法
    // 缓和曲线内移距离p，第一参数曲线长度ls
    private static final UtilsMath.TermFunction termShiftDistance = (n, ls, params) -> {
        double r = params[0];  // 圆曲线半径

        double numerator = Math.pow(-1, n) * Math.pow(ls, 2 * n + 2);
        double denominator = 2 * (4 * n + 3) * UtilsMath.factorial(2 * n + 2) * Math.pow(2 * r, 2 * n + 1);
        // TODO:factorial和pow可能需要注意溢出（尤其是缓和曲线长度较大的情况下），可能可以考虑修改公式

        return numerator / denominator;
    };

    // 缓和曲线切线增长q，第一参数曲线长度ls
    private static final UtilsMath.TermFunction termTangleOffset = (n, ls, params) -> {
        double r = params[0];  // 圆曲线半径

        double numerator = Math.pow(-1, n) * Math.pow(ls, 2 * n + 1);
        double denominator = 2 * (4 * n + 1) * UtilsMath.factorial(2 * n + 1) * Math.pow(2 * r, 2 * n);

        return numerator / denominator;
    };

    // 缓和曲线X坐标，自变量走行距离lw
    private static final UtilsMath.TermFunction termTransArcX = (n, lw, params) -> {
        double a = params[0];  // 曲率变化率

        double numerator = Math.pow(-1, n) * Math.pow(a, 2 * n) * Math.pow(lw, 4 * n + 1);
        double denominator = UtilsMath.factorial(2 * n) * (4 * n + 1) * Math.pow(2, 2 * n);

        return numerator / denominator;
    };

    // 缓和曲线Y坐标，自变量走行距离lw
    private static final UtilsMath.TermFunction termTransArcY = (n, lw, params) -> {
        double a = params[0];  // 曲率变化率

        double numerator = Math.pow(-1, n) * Math.pow(a, 2 * n + 1) * Math.pow(lw, 4 * n + 3);
        double denominator = UtilsMath.factorial(2 * n + 1) * (4 * n + 3) * Math.pow(2, 2 * n + 1);

        return numerator / denominator;
    };

    // 确定两段双螺旋曲线的起点
    private static TransArcStartResult getStartsOfEulerArcs(
            EastNorth A, EastNorth B, EastNorth C,
            double enCurveRadius, double enTransArcLength  // 圆曲线半径（内圆R）、缓和段长度（ls）
    ) {
        /// 先计算deflectionAngleRad路径偏转角（α），也就是总圆心角，拐点夹角+α=180°
        // 获取node坐标
        double[] a = new double[]{A.getX(), A.getY()};  // A起点     B → C
        double[] b = new double[]{B.getX(), B.getY()};  // B拐点     ↑
        double[] c = new double[]{C.getX(), C.getY()};  // C终点     A
        // 方向向量
        double[] v1 = UtilsMath.sub(a, b);  // BA向量
        double[] v2 = UtilsMath.sub(c, b);  // BC向量
        double n1 = UtilsMath.norm(v1);     // BA长度
        double n2 = UtilsMath.norm(v2);     // BC长度
        if (n1 < 1e-9 || n2 < 1e-9) return null;  // 检查向量有效性（不能太短近乎于点挤在一起）
        double[] u1 = UtilsMath.mul(v1, 1.0 / n1);  // BA单位向量
        double[] u2 = UtilsMath.mul(v2, 1.0 / n2);  // BC单位向量
        // 拐点夹角（方向向量点积取acos）和偏转角α
        double dp = Math.max(-1.0, Math.min(1.0, UtilsMath.dot(u1, u2)));  // 点积（限制在[-1,1]范围内）
        double theta = Math.acos(dp);  // 夹角（弧度[0,π]）
        double deflectionAngleRad = Math.PI - theta;  // 路线偏转角（α）
        // 判断左右拐
        int leftRight;
        double crossz = u1[0] * u2[1] - u1[1] * u2[0];  // 向量叉积的Z分量，<0左拐，>0右拐（注意crossz是BA×BC不是AB×BC，所以这里正负判断是反过来的）
        if (crossz < 0) leftRight = LEFT; else leftRight = RIGHT;

        /// 计算（从A侧缓曲线起点开始计算双螺旋1、从C侧缓曲线终点倒过来计算双螺旋2、中间的圆弧）
        // 计算两侧切线长
        double enShiftDistance = UtilsMath.sumSeriesAtVarValue(  // 内移距离（p）：外圆内圆间距
                termShiftDistance,
                TERM_MAX,
                enTransArcLength,
                enCurveRadius
        );
        double enTangleOffset = UtilsMath.sumSeriesAtVarValue(  // 切线增长（q，或m表示）
                termTangleOffset,
                TERM_MAX,
                enTransArcLength,
                enCurveRadius
        );
        double enTangentLength = (enCurveRadius + enShiftDistance) * Math.tan(deflectionAngleRad / 2) + enTangleOffset;  // 切线长
        // 按照切线长确定起点
        if (enTangentLength > n1 || enTangentLength > n2) return null;
        double[] zh1 = UtilsMath.add(b, UtilsMath.mul(u1, enTangentLength));  // A侧直缓切点
        double[] zh2 = UtilsMath.add(b, UtilsMath.mul(u2, enTangentLength));  // C侧直缓切点

        // 计算两侧的起点角（坐标角度：以东为0，北正南负）
        double startAngle1Rad = Math.atan2(-u1[1], -u1[0]);  // 注意atan2接收顺序是y,x，然后注意A侧的度数是AB不是BA所以有负号
        double startAngle2Rad = Math.atan2(-u2[1], -u2[0]);  // 同理，CB的方向

        return new TransArcStartResult(
                new EastNorth(zh1[0], zh1[1]), new EastNorth(zh2[0], zh2[1]),
                startAngle1Rad, startAngle2Rad,
                leftRight
        );
    }

    // 绘制一段未旋转、移动的螺旋曲线（完整的过渡曲线包含2段）
    private static SingleEulerArcResult getUnrotatedEulerArc(
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength,  // 每个桩（节点）之间的距离
            int leftRight  // 往左走往右走
    ) {
        if (leftRight != LEFT && leftRight != RIGHT)  // 哇，还有凉面派
            throw new IllegalArgumentException(I18n.tr("getUnrotatedTransitionArc: Unexpected leftRight arg."));
        if (enChainageLength > enTransArcLength)  // 桩距不能比总长度还大
            throw new IllegalArgumentException(I18n.tr("getUnrotatedTransitionArc: enChainageLength > enTransArcLength."));

        // double deflectionAngleRad = Math.toRadians(deflectionAngleDeg);
        List<EastNorth> result = new ArrayList<>();
        int totalChainage = (int) Math.ceil(enTransArcLength / enChainageLength);  // 总桩数（向上取整）
        double curvatureChangeRate = 1 / (enCurveRadius * enTransArcLength);  // 曲率变化率
        // 曲线坐标（回旋线）
        for (int chainage = 0; chainage <= totalChainage; chainage ++) {
            double walkingLength = chainage * enChainageLength;  // 走行距离
            if (walkingLength > enTransArcLength || chainage == totalChainage)
                walkingLength = enTransArcLength;  // 曲线终点

            // 按照公式求和级数
            double x = UtilsMath.sumSeriesAtVarValue(
                    termTransArcX,
                    TERM_MAX,
                    walkingLength,  // 自变量当前走行长度l
                    curvatureChangeRate
            );
            double y = UtilsMath.sumSeriesAtVarValue(
                    termTransArcY,
                    TERM_MAX,
                    walkingLength,
                    curvatureChangeRate
            );
            result.add(new EastNorth(x, y * leftRight));  // 坐标系↑→，y正（LEFT=1）曲线左转，y负（RIGHT=-1）曲线右转
        }

        // 计算终点处的切角
        double endTangentAngleRad = leftRight * (curvatureChangeRate * enTransArcLength * enTransArcLength / 2.0);

        return new SingleEulerArcResult(result, 0.0, UtilsMath.normAngle(endTangentAngleRad));
    }

    // 将未旋转、移动的螺旋曲线（起点在原点）绕原点旋转，然后移动到曲线的开始处
    private static SingleEulerArcResult rotateAndMoveEulerArc(
            EastNorth start, double startAngleRad,  // 起点坐标和入曲线角度，坐标角度：以东为0，北正南负
            SingleEulerArcResult transArc  // 没有移动旋转的单段双螺旋曲线
    ) {
        List<EastNorth> unrotatedArc = transArc.arcNodes;
        List<EastNorth> result = new ArrayList<>(unrotatedArc.size());

        double cos = Math.cos(startAngleRad);
        double sin = Math.sin(startAngleRad);

        for (EastNorth node : unrotatedArc) {
            double rx = node.getX() * cos - node.getY() * sin;
            double ry = node.getX() * sin + node.getY() * cos;
            result.add(new EastNorth(
                    start.getX() + rx,
                    start.getY() + ry
            ));
        }

        // 计算终点处的切角（原角+旋转度数）
        double endTangentAngleRad = UtilsMath.normAngle(transArc.endTangentAngleRad + startAngleRad);

        return new SingleEulerArcResult(result, startAngleRad, endTangentAngleRad);
    }

    // 画圆曲线段
    private static List<EastNorth> getCircularArc(
            EastNorth start,  // 起点坐标
            double startAngleRad, double endAngleRad,  // 入、出曲线角度，坐标角度：以东为0，北正南负
            double enRadius, int leftRight,
            double enChainageLength
    ) {
        List<EastNorth> result = new ArrayList<>();

        // 圆心（从起点沿法线方向移动半径距离得到圆心）
        double normalAngleStartRed = UtilsMath.normAngle(startAngleRad + leftRight * Math.PI / 2);  // 起点法线方向，左转+90°右转-90°（坐标角度）
        double[] center = {
                start.getX() + enRadius * Math.cos(normalAngleStartRed),
                start.getY() + enRadius * Math.sin(normalAngleStartRed)
        };
        // 起止点相对于圆心的角度（通过起点法向角度取反获得，是坐标角度）和圆心角
        double ang1 = UtilsMath.normAngle(startAngleRad + leftRight * Math.PI / 2 + Math.PI);
        double ang2 = UtilsMath.normAngle(endAngleRad + leftRight * Math.PI / 2 + Math.PI);
        double centralAngleRad = UtilsMath.normAngle(ang2 - ang1);  // 防止AB、BC跨±180°线时画优弧（「<」这种情况）
        // UtilsUI.testMsgWindow("函数原始输入：" + Math.toDegrees(startAngleRad) + " " + Math.toDegrees(endAngleRad) + "\n"
        //         + "起止点相对于圆心的角度" + Math.toDegrees(ang1) + " " + Math.toDegrees(ang2) + "\n"
        //         + "没归一化的角度差" + Math.toDegrees(ang2 - ang1) + " 归一化的角度差" + Math.toDegrees(UtilsMath.normAngle(ang2 - ang1))
        // );

        // 计算点数
        int numAngleSteps = Math.abs((int) (enRadius * centralAngleRad / enChainageLength));

        // 画圆弧
        for (int i = 0; i <= numAngleSteps; i ++){
            double tt = (double) i / numAngleSteps;    // 插值参数 [0,1]
            double ang = ang1 + centralAngleRad * tt;    // 当前角度
            double x = center[0] + enRadius * Math.cos(ang);  // 圆弧点X坐标
            double y = center[1] + enRadius * Math.sin(ang);  // 圆弧点Y坐标
            result.add(new EastNorth(x, y));
        }

        return result;
    }

    // 绘制一条完整的缓和曲线（汇总3段子曲线，返回null表示失败）
    public static ArrayList<EastNorth> getTransCurve(
            EastNorth A, EastNorth B, EastNorth C,
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength  // 每个桩（节点）之间的距离
    ) throws IllegalArgumentException
    {
        // 确定两段双螺旋曲线的起点
        TransArcStartResult transArcStarts = getStartsOfEulerArcs(A, B, C, enCurveRadius, enTransArcLength);
        if (transArcStarts == null) return null;

        /// A侧螺旋线（从A侧直缓切点顺着画）
        // 绘制
        SingleEulerArcResult unrotatedTransArcA = getUnrotatedEulerArc(
                enCurveRadius, enTransArcLength,
                enChainageLength,
                transArcStarts.leftRight
        );
        // 旋转、移动
        SingleEulerArcResult rotatedTransArcA = rotateAndMoveEulerArc(
                transArcStarts.startA,
                transArcStarts.startAngleARad,
                unrotatedTransArcA
        );
        /// C侧螺旋线（从C侧直缓切点开始倒着画）
        // 绘制
        SingleEulerArcResult unrotatedTransArcC = getUnrotatedEulerArc(
                enCurveRadius, enTransArcLength,
                enChainageLength,
                -transArcStarts.leftRight  // C侧是倒回来画的，与A到C方向的左右相反
        );
        // 旋转、移动
        SingleEulerArcResult rotatedTransArcC = rotateAndMoveEulerArc(
                transArcStarts.startC,
                transArcStarts.startAngleCRad,
                unrotatedTransArcC
        );
        /// 圆曲线
        List<EastNorth> circularArc = getCircularArc(
                rotatedTransArcA.arcNodes.getLast(),
                rotatedTransArcA.endTangentAngleRad,  // A侧出曲线的方向
                UtilsMath.normAngle(rotatedTransArcC.endTangentAngleRad + Math.PI),  // C侧双螺旋是倒过来画的，所以它绘制意义上的（终点）出曲线方向取反向才是行进方向A→B→C的角度
                enCurveRadius, transArcStarts.leftRight,
                enChainageLength
        );
        /// 拼接
        // 整理用于拼接的点
        List<EastNorth> transArcA = new ArrayList<>(rotatedTransArcA.arcNodes);
        List<EastNorth> transArcC = new ArrayList<>(rotatedTransArcC.arcNodes);
        if (transArcA.size() < 2 || circularArc.size() < 2 || transArcC.size() < 2) return null;  // 曲线不完整，绘制失败
        transArcA.removeFirst();  // 不要ArcA的最后一个点（=圆曲线第一个点）
        Collections.reverse(transArcC);  // 倒着画的原地逆序正回来
        transArcC.removeFirst();  // 正序之后不要第一个点（=圆曲线最后一个点）

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
     * @param surfaceChainage 桩距（节点间距，米）
     * @return 包含新节点列表和失败节点ID的DrawingNewNodeResult
     */
    public static DrawingNewNodeResult buildTransitionCurvePolyline(
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

            try {
                // JOSM用Mercator投影的NorthEast坐标等角不等距，需要重算距离，以拐点B取维度计算
                double latB = nodes.get(i + 1).getCoor().lat();
                double enRadius = UtilsMath.surfaceDistanceToEastNorth(surfaceRadius, latB);
                double enLength = UtilsMath.surfaceDistanceToEastNorth(surfaceLength, latB);
                double enChainage = UtilsMath.surfaceDistanceToEastNorth(surfaceChainage, latB);
                // 有EN长度之后继续算圆弧
                ArrayList<EastNorth> transCurve = getTransCurve(  // 为每个拐角计算缓和曲线
                        A, B, C,
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

                ArrayList<EastNorth> transCurve = getTransCurve(
                        nodesEN.get(nPts - 2), nodesEN.get(0), nodesEN.get(1),  // nodesEN.get(0) = getFirst() == nodesEN.get(-1)
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
            return new DrawingNewNodeResult(new ArrayList<>(nodes), failedNodes);
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
                    for (int j = 1; j < curve.size(); j++) {
                        finalNodes.add(new Node(UtilsMath.toLatLon(curve.get(j))));
                    }
                } else finalNodes.add(nodes.get(i + 1));
            }
        }

        return new DrawingNewNodeResult(finalNodes, failedNodes);
    }

    // 打包双螺旋曲线的起始点、起始偏角
    private static final class TransArcStartResult {
        public final EastNorth startA;
        public final EastNorth startC;
        public final double startAngleARad;  // AB方向，坐标角度：以东为0，北正南负
        public final double startAngleCRad;  // CB方向，坐标角度：以东为0，北正南负
        public final int leftRight;

        TransArcStartResult(
                EastNorth startA, EastNorth startC,
                double startAngleARad, double startAngleCRad,
                int leftRight) {
            this.startA = startA;
            this.startC = startC;
            this.startAngleARad = startAngleARad;
            this.startAngleCRad = startAngleCRad;
            this.leftRight = leftRight;
        }
    }
    // 打包单段螺旋曲线的节点、起始和终点偏角
    private static final class SingleEulerArcResult {
        public final List<EastNorth> arcNodes;
        public final double startTangentAngleRad;  // 入曲线方向，坐标角度：以东为0，北正南负
        public final double endTangentAngleRad;  // 出曲线方向，坐标角度：以东为0，北正南负

        SingleEulerArcResult(List<EastNorth> arcNodes, double startTangentAngleRad, double endTangentAngleRad) {
            this.arcNodes = arcNodes;
            this.startTangentAngleRad = startTangentAngleRad;
            this.endTangentAngleRad = endTangentAngleRad;
        }
    }

    /*TODO：边缘情况检查
    注意极端几何：LS太长（导致圆曲线没了）、R太小、α太小
    getUnrotatedEulerArc、getCircularArc会不会只返回1个点
    处理各种null的情况
     */
}


