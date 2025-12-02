package yakxin.columbina;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import yakxin.columbina.data.FilletResult;

import java.util.ArrayList;
import java.util.List;


// 圆角计算器
public class FilletGenerator {
    /// 坐标转换
    private static EastNorth toEastNorth(LatLon ll) {  // 经纬度转距离坐标
        return ProjectionRegistry.getProjection().latlon2eastNorth(ll);
    }
    private static LatLon toLatLon(EastNorth en) {  // 距离坐标转经纬度
        return ProjectionRegistry.getProjection().eastNorth2latlon(en);
    }

    /// 向量数学
    private static double norm(double[] v) { return Math.hypot(v[0], v[1]); }  // 模长
    private static double dot(double[] a, double[] b) { return a[0]*b[0] + a[1]*b[1]; }  // 点积
    private static double[] sub(double[] a, double[] b) { return new double[]{a[0]-b[0], a[1]-b[1]}; }  // 减法
    private static double[] add(double[] a, double[] b) { return new double[]{a[0]+b[0], a[1]+b[1]}; }  // 加法
    private static double[] mul(double[] a, double s) { return new double[]{a[0]*s, a[1]*s}; }  // 缩放

    /// 圆角算法
    // 绘制一个圆角
    public static List<EastNorth> filletArcEN(
            EastNorth A, EastNorth B, EastNorth C,
            double R, double angleStep, int maxNumPoints,
            double minAngleDeg, double maxAngleDeg
    ) {
        // 获取node坐标
        double[] a = new double[]{A.getX(), A.getY()};  // A起点     B → C
        double[] b = new double[]{B.getX(), B.getY()};  // B拐点     ↑
        double[] c = new double[]{C.getX(), C.getY()};  // C终点     A

        // 方向向量
        double[] v1 = sub(a, b);  // BA向量
        double[] v2 = sub(c, b);  // BC向量
        double n1 = norm(v1);     // BA长度
        double n2 = norm(v2);     // BC长度
        if (n1 < 1e-9 || n2 < 1e-9) return null;  // 检查向量有效性（不能太短近乎于点挤在一起）

        double[] u1 = mul(v1, 1.0 / n1);  // BA单位向量
        double[] u2 = mul(v2, 1.0 / n2);  // BC单位向量

        // 拐点夹角（方向向量点积取acos）
        double dp = Math.max(-1.0, Math.min(1.0, dot(u1, u2)));  // 点积（限制在[-1,1]范围内）
        double theta = Math.acos(dp);  // 夹角（弧度[0,π]）
        // 检查张角有效性
        double minAngleRad = Math.toRadians(minAngleDeg);
        double maxAngleRad = Math.toRadians(maxAngleDeg);
        if (theta < minAngleRad || theta > maxAngleRad) return null;  // 自定义角度控制
        if (theta < 1e-9 || theta > Math.PI - 1e-9) return null;  // θ为0说明成了发卡角，θ为π说明张角基本是直线

        // 圆弧的起点和终点（切点）
        double t = R / Math.tan(theta / 2.0);  // 从B到两侧切点的距离
        if (t > n1 - 1e-9 || t > n2 - 1e-9) return null;  // 检查半径是否过大
        double[] T1 = add(b, mul(u1, t));  // BA上的切点坐标T1
        double[] T2 = add(b, mul(u2, t));  // BC上的切点坐标T2

        // 圆弧圆心坐标
        double d = R / Math.sin(theta / 2.0);  // B到圆心的距离
        double[] bis = add(u1, u2);  // 角平分线方向
        if (norm(bis) < 1e-12) return null;  // 角平分线是否有效
        double[] center = add(b, mul(bis, d / norm(bis))); // 圆心坐标

        // 圆弧起始和结束角度
        double ang1 = Math.atan2(T1[1] - center[1], T1[0] - center[0]);  // 圆心到T1的角度
        double ang2 = Math.atan2(T2[1] - center[1], T2[0] - center[0]);  // 圆心到T2的角度

        // 圆弧方向（顺时针或逆时针）
        double crossz = u1[0] * u2[1] - u1[1] * u2[0];  // 向量叉积的Z分量
        if (crossz < 0) {
            // 逆时针方向，确保ang2 > ang1
            if (ang2 < ang1) ang2 += 2*Math.PI;
        } else {
            // 顺时针方向，确保ang2 < ang1
            if (ang2 > ang1) ang2 -= 2*Math.PI;
        }

        // 生成圆弧上的点
        int numAngleSteps = Math.max(  // 步进段数，不算开头结尾的点数=步进段数-2
                Math.min((int) (theta / Math.toRadians(angleStep)), maxNumPoints + 1),  // 最大50点的话步进有51段
                1
        );  // 至少1段0点（相当于切角），至多maxNumPoints+1段maxNumPoints点
        // TODO：切角可以快速实现了
        List<EastNorth> arc = new ArrayList<>();
        for (int i = 0; i <= numAngleSteps; i ++){
            double tt = (double) i / numAngleSteps;    // 插值参数 [0,1]
            double ang = ang1 + (ang2 - ang1) * tt;    // 当前角度
            double x = center[0] + R * Math.cos(ang);  // 圆弧点X坐标
            double y = center[1] + R * Math.sin(ang);  // 圆弧点Y坐标
            arc.add(new EastNorth(x, y));
        }
        return arc;
    }

    public static FilletResult buildSmoothPolyline(Way way, double radiusMeters) {
        return buildSmoothPolyline(way, radiusMeters, 20);
    }
    public static FilletResult buildSmoothPolyline(Way way, double radiusMeters, int maxPointNum) {
        return buildSmoothPolyline(way, radiusMeters, 1.0, maxPointNum, 1e-9, 180 - 1e-9);
    }
    // 汇总所有的圆角
    public static FilletResult buildSmoothPolyline(
            Way way,
            double radiusMeters, double angleStep, int maxPointNum,
            double minAngleDeg, double maxAngleDeg
    ) {
        List<Long> failedNodes = new ArrayList<>();
        // 获取路径的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());
        int nPts = nodes.size();
        if (nPts < 2) return null;  // 路径至少需要2个点

        // 将所有节点转换为平面坐标
        List<EastNorth> en = new ArrayList<>();
        for (Node n : nodes) en.add(toEastNorth(n.getCoor()));

        // 为每个拐角预计算圆角
        List<double[]> T1s = new ArrayList<>();  // 存储每个拐角的第一个切点
        // List<double[]> T2s = new ArrayList<>();  // 存储每个拐角的第二个切点
        List<List<EastNorth>> arcs = new ArrayList<>();  // 存储每个拐角的圆弧
        for (int i = 0; i < nPts - 2; i ++){
            EastNorth A = en.get(i);      // 起点    B → C
            EastNorth B = en.get(i + 1);  // 拐点    ↑
            EastNorth C = en.get(i + 2);  // 终点    A

            List<EastNorth> arc = filletArcEN(  // 为每个拐角生成PNum个点的圆弧
                    A, B, C,
                    radiusMeters, angleStep, maxPointNum,
                    minAngleDeg, maxAngleDeg
            );

            if (arc == null) {  // 该拐角没有生成圆角（半径过大或角度问题）
                T1s.add(null);
                // T2s.add(null);
                arcs.add(null);
                failedNodes.add(nodes.get(i).getUniqueId());
            } else {  // 存储切点和圆弧
                EastNorth t1 = arc.getFirst(); // EastNorth t2 = arc.getLast();
                T1s.add(new double[]{t1.getX(), t1.getY()});
                // T2s.add(new double[]{t2.getX(), t2.getY()});
                arcs.add(arc);
            }
        }
        if (way.isClosed()) {  // 闭合曲线首尾相连的首末点曲线
            List<EastNorth> arcEnd = filletArcEN(
                    en.get(nPts - 2), en.get(0), en.get(1),  // en.get(0) == en.get(-1)
                    radiusMeters, angleStep, maxPointNum,
                    minAngleDeg, maxAngleDeg
            );
            if (arcEnd == null) {
                T1s.add(null);
                // T2s.add(null);
                arcs.add(null);
                failedNodes.add(nodes.getFirst().getUniqueId());
            } else {
                EastNorth t1End = arcEnd.getFirst(); // EastNorth t2End = arcEnd.getLast();
                T1s.add(new double[]{t1End.getX(), t1End.getY()});
                // T2s.add(new double[]{t2End.getX(), t2End.getY()});
                arcs.add(arcEnd);
            }
        }

        // 最终的经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        // 添加起始点：如果原路径闭合，且首末点有曲线，则以首末点曲线（arcs最后一个）终点为整个新路径起点；否则使用原路径第一个节点
        boolean useLastArcLastNode = way.isClosed() && arcs.getLast() != null;
        if (useLastArcLastNode) finalNodes.add(new Node(toLatLon(arcs.getLast().getLast())));
        else finalNodes.add(way.getNode(0));
        // 遍历除最后一个节点以外所有路径节点，用圆弧替换拐角
        for (int i = 0; i < nPts - 2; i ++) {
            if (arcs.get(i) != null) {  // 检查本次的拐角B（i+1）是否有有效圆角（圆角编号=A编号=i），圆弧存在则使用圆角路径
                double[] T1 = T1s.get(i);
                Node curveFirst = new Node(toLatLon(new EastNorth(T1[0], T1[1])));
                // 添加圆弧上第一个切点（如果与上个点不同）
                if (!finalNodes.getLast().equals(curveFirst)) finalNodes.add(curveFirst);
                // 添加圆弧上其余点（跳过第一个点，避免重复）
                List<EastNorth> arc = arcs.get(i);
                for (int k = 1; k < arc.size(); k ++)
                    finalNodes.add(new Node(toLatLon(arc.get(k))));
            } else {  // 圆弧不存在则直接将拐点B加进来
                finalNodes.add(way.getNode(i + 1));
            }
        }
        // 终点处理
        if (way.isClosed()) {
            if (arcs.getLast() != null) {  // 如果原路径闭合，且首末点有曲线，拼上最后一条曲线并连上起点
                List<EastNorth> arcClosedEnd = arcs.getLast();  // 对于闭合路径nPts-2倒数第2个点，nPts-1最后一个点=0起点，1表示第2个点
                for (int k = 0; k < arcClosedEnd.size(); k ++)
                    finalNodes.add(new Node(toLatLon(arcClosedEnd.get(k))));
                finalNodes.add(finalNodes.getFirst());
            } else finalNodes.add(way.getNode(0));
        } else {
            finalNodes.add(way.getNode(way.getNodesCount() - 1));
        }

        return new FilletResult(finalNodes, failedNodes);
        // 正式绘制前注意去重
    }
}


