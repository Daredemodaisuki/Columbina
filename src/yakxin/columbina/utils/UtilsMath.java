package yakxin.columbina.utils;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaException;

import java.util.ArrayList;
import java.util.List;

public class UtilsMath {
    public static final int LEFT = 1;
    public static final int RIGHT = -LEFT;  // -1


    /// 坐标转换
    public static EastNorth toEastNorth(LatLon ll) {  // 经纬度转距离坐标
        return ProjectionRegistry.getProjection().latlon2eastNorth(ll);
    }

    public static LatLon toLatLon(EastNorth en) {  // 距离坐标转经纬度
        return ProjectionRegistry.getProjection().eastNorth2latlon(en);
    }

    public static double surfaceDistanceToEastNorth(double surfaceDistance, double lat) throws ColumbinaException {
        double cosLat = Math.cos(Math.toRadians(lat));
        // 避免cosLat为0（极点附近）
        if (Math.abs(cosLat) < 1e-9) {
            throw new ColumbinaException(I18n.tr("Too near the pole, failed to calculate EN distance."));
        }
        return surfaceDistance / cosLat;
    }

    public static double eastNorthDistanceToSurface(double enDistance, double lat) {
        double cosLat = Math.cos(Math.toRadians(lat));
        return enDistance * cosLat;
    }


    /// 向量数学
    public static double norm(double[] v) { return Math.hypot(v[0], v[1]); }  // 模长

    public static double dot(double[] a, double[] b) { return a[0]*b[0] + a[1]*b[1]; }  // 点积

    public static double[] sub(double[] a, double[] b) { return new double[]{a[0]-b[0], a[1]-b[1]}; }  // 减法

    public static double[] add(double[] a, double[] b) { return new double[]{a[0]+b[0], a[1]+b[1]}; }  // 加法

    public static double[] mul(double[] a, double s) { return new double[]{a[0]*s, a[1]*s}; }  // 缩放

    public static double[] getUnitVec(double[] vec) { return mul(vec, 1 / norm(vec)); }  // 取单位向量

    public static double normAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    // 获取向量坐标角度：以东为0，（逆时针）北正（顺时针）南负，区间在[-pi, pi]，±pi等同处理
    public static double getVecBearingRad(double[] vec) {
        return Math.atan2(vec[1], vec[0]);  // atan2(y, x) 返回的是从x轴正方向逆时针到向量的角度
    }

    // 获取从A到B的方向角
    public static double getBearingRadFromAtoB(double[] a, double[] b) {
        return Math.atan2(b[1] - a[1], b[0] - a[0]);
    }

    // 向量a到b是左拐还是右拐（向量叉积的Z分量判断法）
    // 正数表示左拐（逆时针），负数表示右拐（顺时针），0表示共线
    public static int getLeftRight(double[] a, double[] b) {
        double cross = a[0]*b[1] - a[1]*b[0];
        if (Math.abs(cross) < 1e-10) return 0;  // 共线
        return cross > 0 ? LEFT : RIGHT;
    }

    // 计算两个向量之间的夹角（0到π之间）
    public static double getAngleRadBetweenVec(double[] a, double[] b) {
        double cosTheta = dot(a, b) / (norm(a) * norm(b));
        // 防止浮点数误差导致cosTheta超出[-1,1]
        cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
        return Math.acos(cosTheta);
    }


    /// 复杂符号
    // 在指定位置的级数求和
    @FunctionalInterface  // 级数中单项函数的抽象接口
    public interface TermFunction {
        public abstract double compute(int termN, double independentVar, double... params);
    }
    public static double sumSeriesAtVarValue(
            TermFunction termFunc,
            int termMax,  // 求和项数
            double independentVar,  // 输入的自变量或第一参数（确定值）
            double... params  // 其他参数
    ) {
        double result = 0.0;
        for (int n = 0; n <= termMax; n ++) {
            result += termFunc.compute(n, independentVar, params);
        }
        return result;
    }

    // 阶乘（注意k到20左右可能溢出）
    public static long factorial(int k) {
        long result = 1;
        for (int i = 2; i <= k; i++) result *= i;
        return result;
    }


    /// 几何相关
    // 根据拐角两侧线段长度和夹角计算这个拐角的最大圆角半径
    public static double getMaxRadiusForCorner(double lenA, double lenC, double thetaRad) {
        return Math.min(lenA, lenC) * Math.tan(thetaRad / 2);
    }

    // 从起点出发，沿指定角度行走指定距离，得到新的点
    public static double[] walkAlongAngleDistance(double[] startPoint, double angleRad, double enDistance) {
        double deltaE = enDistance * Math.cos(angleRad);  // 东方向增量
        double deltaN = enDistance * Math.sin(angleRad);  // 北方向增量
        return new double[]{startPoint[0] + deltaE, startPoint[1] + deltaN};
    }

    // 指定圆心、半径、入曲线方向角、出曲线方向角绘制这段圆弧
    public static List<EastNorth> getCircleArcPointsWithBearingRad(
            EastNorth center, double radius,
            double startBearingRad, double endBearingRad,
            int segments,  // 曲线有多少桩段，也就是不计首尾的节点数+1
            int leftRight  // 进入方向1左拐，2右拐
    ) {
        List<EastNorth> points = new ArrayList<>();

        // 计算圆弧总角度（根据转弯方向确定旋转方向）
        startBearingRad = normAngle(startBearingRad); endBearingRad = normAngle(endBearingRad);  // 确保角度在[-π, π]范围内
        double totalAngle;
        if (leftRight == LEFT) {
            // 左拐：逆时针，endAngle应该大于startAngle
            if (endBearingRad <= startBearingRad) endBearingRad += 2 * Math.PI;
            totalAngle = endBearingRad - startBearingRad;
        } else {
            // 右拐：顺时针，endAngle应该小于startAngle
            if (endBearingRad >= startBearingRad) endBearingRad -= 2 * Math.PI;
            totalAngle = startBearingRad - endBearingRad;
        }

        // 计算圆弧
        double arcLength = radius * totalAngle;  // 计算圆弧长度
        // int segments = Math.max(2, (int) Math.ceil(arcLength / segments));  // 计算需要的节点数量（桩之间的段数）

        double centerToStartBearingRad = normAngle(startBearingRad - leftRight * 0.5 * Math.PI);  // 圆心到起始点的角度（左拐-90°）
        double angleStep = totalAngle / segments;  // 计算角度步长
        if (leftRight == RIGHT) angleStep = -angleStep;  // 如果是右拐，角度步长为负
        for (int i = 0; i <= segments; i ++) {  // 生成圆弧上的点
            double currentAngle = centerToStartBearingRad + i * angleStep;
            // 计算圆弧上的点坐标
            double east = center.east() + radius * Math.cos(currentAngle);
            double north = center.north() + radius * Math.sin(currentAngle);
            points.add(new EastNorth(east, north));
        }
        return points;
    }
}


