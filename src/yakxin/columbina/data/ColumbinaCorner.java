package yakxin.columbina.data;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.utils.UtilsMath;

/**
 * 拐角类
 */
public class ColumbinaCorner {
    // 储存节点
    public final ColumbinaEN A;  // A起点     B → C
    public final ColumbinaEN B;  // B拐点     ↑
    public final ColumbinaEN C;  // C终点     A
    // 储存向量
    public final ColumbinaEN AB;
    public final ColumbinaEN BC;
    public final ColumbinaEN BA;
    // 储存长度
    public final double lenBA;
    public final double lenBC;
    // 储存角度
    public final int leftRight;  // A→B→C是左拐（AB到BC是逆时针）还是右拐（AB到BC是顺时针）
    public final double angleRad;  // 夹角（张角，[0, π]）
    public final double deflectionRad;  // 偏转角（[-π, π]）
    // 拐点纬度（en和surface转换用）
    public final double latB;

    /**
     * 从3个ColumbinaEN构建ColumbinaCorner
     * @param a 起点
     * @param b 拐点
     * @param c 终点
     */
    ColumbinaCorner(ColumbinaEN a, ColumbinaEN b, ColumbinaEN c) {
        this.A = a; this.B = b; this.C = c;
        this.AB = new ColumbinaEN(a, b); this.BC = new ColumbinaEN(b, c);
        this.BA = new ColumbinaEN(b, a);
        this.lenBA = AB.length(); this.lenBC = BC.length();
        this.leftRight = AB.turnLeftRightTo(BC);
        this.angleRad = BA.angleRadBetween(BC);
        this.deflectionRad = leftRight * (Math.PI - angleRad);
        this.latB = UtilsMath.toLatLon(B).lat();
    }

    /**
     * 从3个EastNorth构建ColumbinaCorner
     * @param a 起点
     * @param b 拐点
     * @param c 终点
     */
    public ColumbinaCorner(EastNorth a, EastNorth b, EastNorth c) {
        this(new ColumbinaEN(a), new ColumbinaEN(b), new ColumbinaEN(c));
    }

    /**
     * 从路径上指定节点构造ColumbinaCorner（静态工厂）
     * @param way 路径
     * @param indexA 起点A的节点索引
     * @return ColumbinaCorner实体
     * @throws ColumbinaException 通常可能是超界的异常或节点缺失异常
     */
    public static ColumbinaCorner create(Way way, int indexA) throws ColumbinaException {
        Node a, b, c;
        int numNode = way.isClosed() ? way.getNodesCount() - 1 : way.getNodesCount();  // 实际节点数（去除闭合点）
        try{
            a = way.getNode(indexA % numNode);
            b = way.getNode((indexA + 1) % numNode);
            c = way.getNode((indexA + 2) % numNode);
        } catch (Exception exWay) {  // 以防万一，捕获如访问超界等异常并包装成ColumbinaException抛出
            throw new ColumbinaException("Failed to create ColumbinaCorner: " + exWay.getMessage());
        }
        if (a == null || b == null || c == null)
            throw new ColumbinaException("Get null Node when creating ColumbinaCorner from Way.");

        return new ColumbinaCorner(
                new ColumbinaEN(a.getEastNorth()), new ColumbinaEN(b.getEastNorth()), new ColumbinaEN(c.getEastNorth())
        );
    }

    /**
     * 获取夹角的角平分线的方向角
     * @return 角平分线方向角
     */
    public double getBisectorBearingRad() {
        // double bearingBA = BA.bearingRad();
        // double bearingBC = BC.bearingRad();
        // return UtilsMath.normAngleRad(  // 最后再归一化回来
        //         (
        //                 (bearingBA < 0 ? bearingBA + 2 * Math.PI: bearingBA)  // 防止跨±180°，对小于0的先+360°
        //                 + (bearingBC < 0 ? bearingBC + 2 * Math.PI: bearingBC)
        //         ) / 2.0  // 除以2
        // );
        return BA.normVec().add(BC.normVec()).bearingRad();
    }
}
