package yakxin.columbina.utils;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaEN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 圆曲线和缓和曲线工具
 */
public class UtilsArc {
    public static final int TERM_MAX = 10;  // 前11项（n从0到10）
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

    /**
     * 确定两段双螺旋曲线的起点
     * @param corner 拐角
     * @param enCurveRadius 圆曲线半径
     * @param enTransArcLength 缓和曲线长度
     * @return 打包好的双螺旋曲线的起始点、起始偏角
     */
    private static TransArcStartResult getStartsOfEulerArcs(
            ColumbinaCorner corner,
            double enCurveRadius, double enTransArcLength  // 圆曲线半径（内圆R）、缓和段长度（ls）
    ) {
        // 计算两侧切距T
        // T = (圆曲线半径R + 内移距离p) * tan(路径偏转角α / 2) + 切线增长q
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
        double enTangentLength =
                (enCurveRadius + enShiftDistance) * Math.tan(Math.abs(corner.deflectionRad) / 2) + enTangleOffset;

        // 按照切距确定起点
        if (enTangentLength > corner.lenBA || enTangentLength > corner.lenBC)
            return null;  // 缓和曲线长度太长导致切距超过拐角两侧距离
        ColumbinaEN zhA = corner.B.walk(corner.BA.bearingRad(), enTangentLength);  // A侧直缓切点
        ColumbinaEN zhC = corner.B.walk(corner.BC.bearingRad(), enTangentLength);  // C侧直缓切点

        // 计算两侧的起点角（坐标角度：以东为0，北正南负）
        double startAngleARad = corner.AB.bearingRad();
        double startAngleCRad = UtilsMath.reverseAngleRad(corner.BC.bearingRad());  // CB的方向，然后注意C侧倒着画的方向角是CB不是BC所以需要取反

        return new TransArcStartResult(
                zhA, zhC,
                enShiftDistance,
                startAngleARad, startAngleCRad,
                corner.leftRight
        );
    }

    /**
     * 绘制一段未旋转、移动的螺旋曲线（完整的过渡曲线包含2段）
     * @param enCurveRadius 圆曲线半径（内圆R）
     * @param enTransArcLength 缓和段长度（ls）
     * @param enChainageLength 每个桩（节点）之间的距离
     * @param leftRight 往左走往右走
     * @return 打包好的单段螺旋曲线的节点、起始和终点偏角
     */
    private static SingleEulerArcResult getUnrotatedEulerArc(
            double enCurveRadius, double enTransArcLength,
            double enChainageLength,
            int leftRight
    ) {
        if (leftRight != UtilsMath.LEFT && leftRight != UtilsMath.RIGHT)  // 哇，还有凉面派
            throw new IllegalArgumentException(I18n.tr("getUnrotatedTransitionArc: Unexpected leftRight arg."));
        if (enChainageLength > enTransArcLength)  // 桩距不能比总长度还大
            throw new IllegalArgumentException(I18n.tr("getUnrotatedTransitionArc: enChainageLength > enTransArcLength."));

        List<EastNorth> result = new ArrayList<>();
        int totalChainage = (int) Math.ceil(enTransArcLength / enChainageLength);  // 总桩数（向上取整）
        enChainageLength = enTransArcLength / totalChainage;  // 按照桩数重算桩距
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

        return new SingleEulerArcResult(result, 0.0, UtilsMath.normAngleRad(endTangentAngleRad));
    }

    /**
     * 将未旋转、移动的螺旋曲线（起点在原点）绕原点旋转，然后移动到曲线的开始处
     * @param start 起始点（A侧的直缓、C侧的缓直）
     * @param startBearingRad 起始方向角，坐标角度：以东为0，北正南负
     * @param transArc 没有移动旋转的单段双螺旋曲线
     * @return 打包好的单段螺旋曲线的节点、起始和终点偏角
     */
    private static SingleEulerArcResult rotateAndMoveEulerArc(
            EastNorth start, double startBearingRad,
            SingleEulerArcResult transArc
    ) {
        List<EastNorth> unrotatedArc = transArc.arcNodes;
        List<EastNorth> result = new ArrayList<>(unrotatedArc.size());

        double cos = Math.cos(startBearingRad);
        double sin = Math.sin(startBearingRad);

        // EastNorth自带的rotate和这里不一样（俩是反着转的），所以不使用它，我们自己写
        for (EastNorth node : unrotatedArc) {
            double rx = node.getX() * cos - node.getY() * sin;
            double ry = node.getX() * sin + node.getY() * cos;
            result.add(new EastNorth(
                    start.getX() + rx,
                    start.getY() + ry
            ));
        }

        // 计算终点处的切角（原角+旋转度数）
        double endTangentAngleRad = UtilsMath.normAngleRad(transArc.endTangentAngleRad + startBearingRad);

        return new SingleEulerArcResult(result, startBearingRad, endTangentAngleRad);
    }

    /**
     * 画圆曲线段
     * <p>指定圆心、半径、入曲线方向角、出曲线方向角绘制这段圆弧
     * @param center 圆心坐标
     * @param radius 半径
     * @param startBearingRad 入曲线方向角，坐标角度：以东为0，北正南负
     * @param endBearingRad 出曲线方向角，坐标角度：以东为0，北正南负
     * @param segments 段数（平滑度）
     * @param leftRight 左转（1）还是右转（-1）
     * @return 节点列表（包含首尾）
     */
    public static List<EastNorth> getCircleArc(
            EastNorth center, double radius,
            double startBearingRad, double endBearingRad,
            int segments,  // 曲线有多少桩段，也就是不计首尾的节点数+1
            int leftRight  // 进入方向1左拐，2右拐
    ) {
        List<EastNorth> points = new ArrayList<>();

        // 计算圆弧总角度（根据转弯方向确定旋转方向）
        startBearingRad = UtilsMath.normAngleRad(startBearingRad); endBearingRad = UtilsMath.normAngleRad(endBearingRad);  // 确保角度在[-π, π]范围内
        double totalAngle;
        if (leftRight == UtilsMath.LEFT) {
            // 左拐：逆时针，endAngle应该大于startAngle
            if (endBearingRad <= startBearingRad) endBearingRad += 2 * Math.PI;
            totalAngle = endBearingRad - startBearingRad;
        } else {
            // 右拐：顺时针，endAngle应该小于startAngle
            if (endBearingRad >= startBearingRad) endBearingRad -= 2 * Math.PI;
            totalAngle = startBearingRad - endBearingRad;
        }

        double centerToStartBearingRad = UtilsMath.normAngleRad(startBearingRad - leftRight * 0.5 * Math.PI);  // 圆心到起始点的角度（左拐-90°）
        double angleStep = totalAngle / segments;  // 计算角度步长
        if (leftRight == UtilsMath.RIGHT) angleStep = -angleStep;  // 如果是右拐，角度步长为负
        for (int i = 0; i <= segments; i ++) {  // 生成圆弧上的点
            double currentAngle = centerToStartBearingRad + i * angleStep;
            // 计算圆弧上的点坐标
            double east = center.east() + radius * Math.cos(currentAngle);
            double north = center.north() + radius * Math.sin(currentAngle);
            points.add(new EastNorth(east, north));
        }
        return points;
    }

    // 绘制一个拐角的完整的缓和曲线（汇总3段子曲线，返回null表示失败）
    public static ArrayList<EastNorth> getTransCurve(
            ColumbinaCorner corner,
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength  // 每个桩（节点）之间的距离
    ) throws IllegalArgumentException
    {
        // 确定两段双螺旋曲线的起点
        TransArcStartResult transArcStarts = getStartsOfEulerArcs(corner, enCurveRadius, enTransArcLength);
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
        // 计算圆心（从B向角平分线方向走(圆曲线半径R + 内移距离p) / sin(张角θ / 2)这个长度）
        double enCenterToB = (enCurveRadius + transArcStarts.enShiftDistance) / Math.sin(corner.angleRad / 2);
        ColumbinaEN center = corner.B.walk(corner.getBisectorBearingRad(), enCenterToB);
        // 计算段数（圆心角）
        double tangentABearingRad = rotatedTransArcA.endTangentAngleRad;
        double tangentCBearingRad = UtilsMath.normAngleRad(rotatedTransArcC.endTangentAngleRad + Math.PI);  // C侧双螺旋是倒过来画的，所以它绘制意义上的（终点）出曲线方向取反向才是行进方向A→B→C的角度
        double centralAngleRad = UtilsMath.normAngleRad(tangentCBearingRad - tangentABearingRad);  // 防止AB、BC跨±180°线时画优弧（「<」这种情况）
        int numAngleSteps = Math.abs((int) (enCurveRadius * centralAngleRad / enChainageLength));
        // 画曲线
        List<EastNorth> circularArc = getCircleArc(
                center, enCurveRadius,
                tangentABearingRad, tangentCBearingRad,
                numAngleSteps, transArcStarts.leftRight
        );
        /// 拼接
        // 整理用于拼接的点
        List<EastNorth> transArcA = new ArrayList<>(rotatedTransArcA.arcNodes);
        List<EastNorth> transArcC = new ArrayList<>(rotatedTransArcC.arcNodes);
        if (transArcA.size() < 2 || circularArc.size() < 2 || transArcC.size() < 2) return null;  // 曲线不完整，绘制失败
        transArcA.removeLast();  // 不要ArcA的最后一个点（=圆曲线第一个点）
        Collections.reverse(transArcC);  // 倒着画的原地逆序正回来
        transArcC.removeFirst();  // 正序之后不要第一个点（=圆曲线最后一个点）

        ArrayList<EastNorth> finalNodes = new ArrayList<>();
        finalNodes.addAll(transArcA);
        finalNodes.addAll(circularArc);
        finalNodes.addAll(transArcC);

        return finalNodes;
    }

    /**
     * 打包双螺旋曲线的起始点、起始偏角
     */
    public static final class TransArcStartResult {
        public final EastNorth startA;
        public final EastNorth startC;
        public final double enShiftDistance;  // 内移距离p（用于算圆心）
        public final double startAngleARad;  // AB方向，坐标角度：以东为0，北正南负
        public final double startAngleCRad;  // CB方向，坐标角度：以东为0，北正南负
        public final int leftRight;

        public TransArcStartResult(
                EastNorth startA, EastNorth startC,
                double enShiftDistance,
                double startAngleARad, double startAngleCRad,
                int leftRight) {
            this.startA = startA;
            this.startC = startC;
            this.enShiftDistance = enShiftDistance;
            this.startAngleARad = startAngleARad;
            this.startAngleCRad = startAngleCRad;
            this.leftRight = leftRight;
        }
    }

    /**
     * 打包单段螺旋曲线的节点、起始和终点偏角
     */
    public static final class SingleEulerArcResult {
        public final List<EastNorth> arcNodes;
        public final double startTangentAngleRad;  // 入曲线方向，坐标角度：以东为0，北正南负
        public final double endTangentAngleRad;  // 出曲线方向，坐标角度：以东为0，北正南负

        public SingleEulerArcResult(List<EastNorth> arcNodes, double startTangentAngleRad, double endTangentAngleRad) {
            this.arcNodes = arcNodes;
            this.startTangentAngleRad = startTangentAngleRad;
            this.endTangentAngleRad = endTangentAngleRad;
        }
    }
}


