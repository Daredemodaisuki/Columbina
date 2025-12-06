package yakxin.columbina.transitionCurve;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.dto.DrawingNewNodeResult;
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


public class TransitionCurveGenerator {
    public static final int LEFT = 1;
    public static final int RIGHT = -LEFT;  // -1

    /// 求和级数的单项：使用匿名函数具体定义TermFunction的抽象compute方法
    // 缓和曲线内移距离p，第一参数曲线长度ls
    private static final UtilsMath.TermFunction termShiftDistance = (n, ls, params) -> {
        double r = params[0];  // 圆曲线半径

        double numerator = Math.pow(-1, n) * Math.pow(ls, 2 * n + 2);
        double denominator = 2 * (4 * n + 3) * UtilsMath.factorial(2 * n + 2) * Math.pow(2 * r, 2 * n + 1);

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
    private static TransArcStartResult getStartsOfTransArcs(
            EastNorth A, EastNorth B, EastNorth C,
            double enCurveRadius, double enTransArcLength  // 圆曲线半径（内圆R）、缓和段长度（ls）
            // int chainageLength  // 每个桩（节点）之间的距离
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
        // 拐点夹角（方向向量点积取acos）和偏转角
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
                5,  // 前6项
                enTransArcLength,
                enCurveRadius
        );
        double enTangleOffset = UtilsMath.sumSeriesAtVarValue(  // 切线增长（q，或m表示）
                termTangleOffset,
                5,
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

    // 绘制一段未旋转、移动的双螺旋曲线（完整的过渡曲线包含2段）
    private static SingleTransArcResult getUnrotatedTransArc(
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength,  // 每个桩（节点）之间的距离
            int leftRight  // 往左走往右走
    ) {
        if (leftRight != LEFT && leftRight != RIGHT)  // 哇，还有凉面派
            throw new IllegalArgumentException(I18n.tr("getUnrotatedTransitionArc: Unexpected leftRight arg."));

        // double deflectionAngleRad = Math.toRadians(deflectionAngleDeg);
        List<EastNorth> result = new ArrayList<>();
        int totalChainage = (int) (enTransArcLength / enChainageLength);  // 总桩数
        double curvatureChangeRate = 1 / (enCurveRadius * enTransArcLength);  // 曲率变化率
        // 曲线坐标（回旋线）
        for (int chainage = 0; chainage <= totalChainage; chainage ++) {
            double walkingLength = chainage * enChainageLength;  // 走行距离
            if (walkingLength > enTransArcLength || chainage == totalChainage)
                walkingLength = enTransArcLength;  // 曲线终点

            // 按照公式求和级数
            double x = UtilsMath.sumSeriesAtVarValue(
                    termTransArcX,
                    5,  // 前6项
                    walkingLength,  // 自变量当前走行长度l
                    curvatureChangeRate
            );
            double y = UtilsMath.sumSeriesAtVarValue(
                    termTransArcY,
                    5,
                    walkingLength,
                    curvatureChangeRate
            );
            result.add(new EastNorth(x, y * leftRight));  // 坐标系↑→，y正（LEFT=1）曲线左转，y负（RIGHT=-1）曲线右转
        }

        // 计算终点处的切角
        double endTangentAngleRad = curvatureChangeRate * enTransArcLength * enTransArcLength / 2.0;

        return new SingleTransArcResult(result, 0.0, endTangentAngleRad);
    }

    // 将未旋转、移动的双螺旋曲线（起点在原点）绕原点旋转，然后移动曲线开始处
    private static SingleTransArcResult rotateAndMoveTransArc(
            EastNorth start, double startAngleRad,  // 起点坐标和入曲线角度，坐标角度：以东为0，北正南负
            SingleTransArcResult transArc  // 没有移动旋转的单段双螺旋曲线
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
        double endTangentAngleRad = transArc.endTangentAngleRad + startAngleRad;

        return new SingleTransArcResult(result, startAngleRad, endTangentAngleRad);
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
        double normalAngleStartRed = startAngleRad + leftRight * Math.PI / 2;  // 起点法线方向，左转+90°右转-90°（坐标角度）
        double[] center = {
                start.getX() + enRadius * Math.cos(normalAngleStartRed),
                start.getY() + enRadius * Math.sin(normalAngleStartRed)
        };
        // 相对于圆心的起止角度（坐标角度）和圆心角
        double ang1 = -(startAngleRad + leftRight * Math.PI / 2);
        double ang2 = -(endAngleRad + leftRight * Math.PI / 2);
        double centralAngleRad = ang2 - ang1;

        // 计算点数
        int numAngleSteps = (int) (enRadius * centralAngleRad / enChainageLength);

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

    // 绘制一条缓和曲线（汇总3段子曲线）
    public static ArrayList<EastNorth> getTransCurve(
            EastNorth A, EastNorth B, EastNorth C,
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength  // 每个桩（节点）之间的距离
    ) {
        // 确定两段双螺旋曲线的起点
        TransArcStartResult transArcStarts = getStartsOfTransArcs(A, B, C, enCurveRadius, enTransArcLength);
        if (transArcStarts == null) return null;

        /// A侧螺旋线（从A侧直缓切点顺着画）
        // 绘制
        SingleTransArcResult unrotatedTransArcA = getUnrotatedTransArc(
                enCurveRadius, enTransArcLength,
                enChainageLength,
                transArcStarts.leftRight
        );
        // 旋转、移动
        SingleTransArcResult rotatedTransArcA = rotateAndMoveTransArc(
                transArcStarts.startA,
                transArcStarts.startAngleARad,
                unrotatedTransArcA
        );
        /// C侧螺旋线（从C侧直缓切点开始倒着画）
        // 绘制
        SingleTransArcResult unrotatedTransArcC = getUnrotatedTransArc(
                enCurveRadius, enTransArcLength,
                enChainageLength,
                -transArcStarts.leftRight  // C侧是倒回来画的，与A到C方向的左右相反
        );
        // 旋转、移动
        SingleTransArcResult rotatedTransArcC = rotateAndMoveTransArc(
                transArcStarts.startC,
                transArcStarts.startAngleCRad,
                unrotatedTransArcC
        );
        /// 圆曲线
        List<EastNorth> circularArc = getCircularArc(
                rotatedTransArcA.arcNodes.getLast(),
                rotatedTransArcA.endTangentAngleRad,  // A侧出曲线的方向
                rotatedTransArcC.endTangentAngleRad + Math.PI,  // C侧双螺旋是倒过来画的，所以它绘制意义上的（终点）出曲线方向取反向才是行进方向A→B→C的角度
                enCurveRadius, transArcStarts.leftRight,
                enChainageLength
        );
        /// 拼接
        // 整理用于拼接的点
        List<EastNorth> transArcA = new ArrayList<>(rotatedTransArcA.arcNodes);
        transArcA.removeFirst();  // 不要ArcA的最后一个点（=圆曲线第一个点）
        List<EastNorth> transArcC = new ArrayList<>(rotatedTransArcC.arcNodes);
        Collections.reverse(transArcC);  // 倒着画的原地逆序正回来
        transArcC.removeFirst();  // 正序之后不要第一个点（=圆曲线最后一个点）

        /*TODO:边缘情况检查
          <p>在特定条件下可能会：
          <p>当 ls < chainageLength（即缓和段很短）
          <p>getUnrotatedTransArc() 只生成一个点
          <p>可能 removeFirst() 会把唯一的点删掉
          <p>→ 缓和段直接消失
          <p>→ 拼接会出错
         */
        ArrayList<EastNorth> finalNodes = new ArrayList<>();
        finalNodes.addAll(transArcA);
        finalNodes.addAll(circularArc);
        finalNodes.addAll(transArcC);

        return finalNodes;
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
    // 打包双螺旋曲线的节点、起始和终点偏角
    private static final class SingleTransArcResult {
        public final List<EastNorth> arcNodes;
        public final double startTangentAngleRad;  // 入曲线方向，坐标角度：以东为0，北正南负
        public final double endTangentAngleRad;  // 出曲线方向，坐标角度：以东为0，北正南负

        SingleTransArcResult(List<EastNorth> arcNodes, double startTangentAngleRad, double endTangentAngleRad) {
            this.arcNodes = arcNodes;
            this.startTangentAngleRad = startTangentAngleRad;
            this.endTangentAngleRad = endTangentAngleRad;
        }
    }
}


