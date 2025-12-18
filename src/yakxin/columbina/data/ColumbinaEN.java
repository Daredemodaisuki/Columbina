package yakxin.columbina.data;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import yakxin.columbina.utils.UtilsArc;

/**
 * 扩展的EastNorth类
 */
public class ColumbinaEN extends EastNorth {
    public static final int COLLINEAR = 0;
    public static final double EPSILON = 1e-10;

    /**
     * 从EastNorth（或ColumbinaEN）实体构造ColumbinaEN
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
     * 从两个EastNorth（或ColumbinaEN）构造ColumbinaEN，获取从A到B的向量
     * <p>等同于 b.sub(a)
     * @param a 点A坐标
     * @param b 点B坐标
     */
    public ColumbinaEN(EastNorth a, EastNorth b) {
        super(b.getX() - a.getX(), b.getY() - a.getY());
    }

    /**
     * 从两个Node构造ColumbinaEN，获取从A到B的向量
     * @param a 点A坐标
     * @param b 点B坐标
     */
    public ColumbinaEN(Node a, Node b) {
        this(a.getEastNorth(), b.getEastNorth());
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
     * @param bearingRad 方向角
     * @param enDistance 东北坐标行进距离
     * @return 新坐标
     */
    public ColumbinaEN walk(double bearingRad, double enDistance) {
        double deltaE = enDistance * Math.cos(bearingRad);
        double deltaN = enDistance * Math.sin(bearingRad);
        return new ColumbinaEN(this.x + deltaE, this.y + deltaN);
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
     * <p>对于零向量取单位向量，这里定义返回零向量避免除0错误
     * @return 单位向量
     */
    public ColumbinaEN normVec() {
        double length = this.length();
        if (length == 0) return this.mul(0);
        return this.mul(1 / length);
    }

    /**
     * 取this（A）和other（B）中点
     * @param other 点B坐标
     * @return 中点坐标
     */
    public ColumbinaEN centerBetween(ColumbinaEN other) {
        return new ColumbinaEN(super.getCenter(other));
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
     * 获取从this（vecA）到other（vecB）的偏转角
     * <p>不使用EastNorth的heading计算，因为它的角度系统和这个插件不一样
     * @param other 点B坐标
     * @return 从this到B的偏转角，坐标角度（以东为0，（逆时针）北正（顺时针）南负，区间在[-pi, pi]，±pi等同处理）
     */
    public double deflectionRadTo(ColumbinaEN other) {
        return Math.atan2(other.getY() - this.y, other.getX() - this.x);
    }

    /**
     * 获取this（vecA）和other（vecB）的夹角
     * <p>对于其中存在零向量的情况，考虑到零向量和任意向量都平行，返回0。
     * @param other 向量B
     * @return 夹角（[0, pi]）
     */
    public double angleRadBetween(ColumbinaEN other) {
        if (this.length() * other.length() == 0) return 0;
        double cosTheta = this.dot(other) / (this.length() * other.length());
        // 防止浮点数误差导致cosTheta超出[-1,1]
        cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
        return Math.acos(cosTheta);
    }

    /**
     * 判断从this（vecA）到other（vecB）是左拐（逆时针偏）还是右拐（顺时针偏）
     * <p>使用向量叉积的Z分量判断法
     * @param other 向量B
     * @return 左（1）右（-1）或共线（0）
     */
    public int turnLeftRightTo(ColumbinaEN other) {
        double cross = this.x * other.y - this.y * other.x;
        if (Math.abs(cross) < EPSILON) return COLLINEAR;
        return cross > 0 ? UtilsArc.LEFT : UtilsArc.RIGHT;
    }
    
    /**
     * 判断B是否在AC连线上且在AC中间
     * <p>根据AB、BC、AC的方向角是否一致进行判断。
     * @param a 点A坐标
     * @param b 点B坐标
     * @param c 点C坐标
     * @return 判断结果
     */
    public static boolean isBOnAC(ColumbinaEN a, ColumbinaEN b, ColumbinaEN c) {
        double bearingAB = new ColumbinaEN(a, b).bearingRad();
        double bearingBC = new ColumbinaEN(b, c).bearingRad();
        double bearingAC = new ColumbinaEN(a, c).bearingRad();
        return (Math.abs(bearingAB - bearingBC) < 10e-6 && Math.abs(bearingAC - bearingBC) < 10e-6);
    }
}


