package yakxin.columbina.data;

import org.openstreetmap.josm.data.coor.EastNorth;
import yakxin.columbina.utils.UtilsMath;

/**
 * 扩展的EastNorth类
 */
public class ColumbinaEN extends EastNorth {
    public static final int COLLINEAR = 0;
    public static final double EPSILON = 1e-10;

    /**
     * 从EastNorth实体构造ColumbinaEN
     * @param en EastNorth实体
     */
    public ColumbinaEN(EastNorth en) {
        super(en.getX(), en.getY());
    }

    /**
     * 从具体数值构造ColumbinaEN
     * @param east x东坐标
     * @param north y北坐标
     */
    public ColumbinaEN(double east, double north) {
        super(east, north);
    }

    /**
     * 从两个ColumbinaEN构造ColumbinaEN，获取从A到B的向量
     * @param a 点A坐标
     * @param b 点B坐标
     */
    public ColumbinaEN(ColumbinaEN a, ColumbinaEN b) {
        super(b.x - a.x, b.y - a.y);
    }

    /**
     * 从两个EastNorth构造ColumbinaEN，获取从A到B的向量
     * @param a 点A坐标
     * @param b 点B坐标
     */
    public ColumbinaEN(EastNorth a, EastNorth b) {
        super(b.getX() - a.getX(), b.getY() - a.getY());
    }


    /**
     * 返回基类EastNorth
     * @return EastNorth
     */
    public EastNorth getEastNorth() {
        return new EastNorth(this.x, this.y);
    }


    /**
     * 从this出发，沿指定角度行进指定距离，得到新的点
     * @param bearingRad 偏转角
     * @param enDistance 东北坐标行进距离
     * @return 新坐标
     */
    public ColumbinaEN walk(double bearingRad, double enDistance) {
        double deltaE = enDistance * Math.cos(bearingRad);
        double deltaN = enDistance * Math.sin(bearingRad);
        return new ColumbinaEN(this.x + deltaE, this.y + deltaN);
    }
    // TODO:旧UtilsMath方法临时移动至此，替换完成后弃用
    public static double[] walk(double[] startPoint, double angleRad, double enDistance) {
        double deltaE = enDistance * Math.cos(angleRad);  // 东方向增量
        double deltaN = enDistance * Math.sin(angleRad);  // 北方向增量
        return new double[]{startPoint[0] + deltaE, startPoint[1] + deltaN};
    }

    /**
     * 计算this（A） + other（B）
     * @param other 点B坐标
     * @return 加和
     */
    public ColumbinaEN add(ColumbinaEN other) {
        return new ColumbinaEN(this.x + other.x, this.y + other.y);
    }

    /**
     * 计算this（A） - other（B）
     * @param other 点B坐标
     * @return 差值
     */
    public ColumbinaEN sub(ColumbinaEN other) {
        return new ColumbinaEN(this.x - other.x, this.y - other.y);
    }

    /**
     * 计算this（vecA） * scale
     * @param scale 缩放因子
     * @return 缩放后的向量
     */
    public ColumbinaEN mul(double scale) {
        return new ColumbinaEN(this.x * scale, this.y * scale);
    }

    /**
     * 取单位向量
     * @return 单位向量
     */
    public ColumbinaEN normVec() {
        double length = this.length();
        return this.mul(1 / length);
    }

    /**
     * 取模长
     * @return 模长
     */
    @Override
    public double length() {
        return super.length();
    }

    /**
     * 计算this（vecA） · other（vecB）点积
     * @param other 向量B
     * @return 点积
     */
    public double dot(ColumbinaEN other) {
        return this.x * other.getX() + this.y * other.getY();
    }

    /**
     * 获取向量相对于原点的方向角
     * <p>不使用EastNorth的heading计算，因为它的角度系统和这个插件不一样
     * @return 坐标角度（以东为0，（逆时针）北正（顺时针）南负，区间在[-pi, pi]，±pi等同处理）
     */
    public double bearingRad() {
        return Math.atan2(this.y, this.x);  // atan2(y, x) 返回的是从x轴正方向逆时针到向量的角度
    }

    /**
     * 获取从this（A）到other（B）的方向角
     * <p>不使用EastNorth的heading计算，因为它的角度系统和这个插件不一样
     * @param other 点B坐标
     * @return 从this到B的方向角，坐标角度（以东为0，（逆时针）北正（顺时针）南负，区间在[-pi, pi]，±pi等同处理）
     */
    public double bearingRadTo(ColumbinaEN other) {
        return Math.atan2(other.getY() - this.y, other.getX() - this.x);
    }

    /**
     * 获取从this（vecA）和other（vecB）的夹角
     * @param other 向量B
     * @return 夹角（[0, pi]）
     */
    public double angleRadBetween(ColumbinaEN other) {
        double cosTheta = this.dot(other) / (this.length() * other.length());
        // 防止浮点数误差导致cosTheta超出[-1,1]
        cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
        return Math.acos(cosTheta);
    }

    /**
     * 判断从this（vecA）和other（vecB）是左拐还是右拐
     * <p>使用向量叉积的Z分量判断法
     * @param other 向量B
     * @return 左（1）右（-1）或共线（0）
     */
    public int turnLeftRightTo(ColumbinaEN other) {
        double cross = this.x * other.y - this.y * other.x;
        if (Math.abs(cross) < EPSILON) return COLLINEAR;
        return cross > 0 ? UtilsMath.LEFT : UtilsMath.RIGHT;
    }
}
