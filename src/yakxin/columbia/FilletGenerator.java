package yakxin.columbia;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

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
    public static List<EastNorth> filletArcEN(EastNorth A, EastNorth B, EastNorth C, double R, int numPoints) {
        // 获取node坐标
        double[] a = new double[]{A.getX(), A.getY()};  // A起点     B → C
        double[] b = new double[]{B.getX(), B.getY()};  // B拐点     ↑
        double[] c = new double[]{C.getX(), C.getY()};  // C终点     A

        // 方向向量
        double[] v1 = sub(a, b);  // BA向量
        double[] v2 = sub(c, b);  // BC向量
        double n1 = norm(v1);     // BA长度
        double n2 = norm(v2);     // BC长度
        if (n1 < 1e-9 || n2 < 1e-9) return null;  // 检查向量有效性

        double[] u1 = mul(v1, 1.0 / n1);  // BA单位向量
        double[] u2 = mul(v2, 1.0 / n2);  // BC单位向量

        // 拐点夹角（方向向量点积取acos）
        double dp = Math.max(-1.0, Math.min(1.0, dot(u1, u2)));  // 点积（限制在[-1,1]范围内）
        double theta = Math.acos(dp);  // 夹角（弧度）
        if (theta < 1e-9 || Math.PI - theta < 1e-9) return null;  // 检查夹角有效性（不能太小或接近180度）

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
        double crossz = u1[0]*u2[1] - u1[1]*u2[0];  // 向量叉积的Z分量
        if (crossz < 0) {
            // 逆时针方向，确保ang2 > ang1
            if (ang2 < ang1) ang2 += 2*Math.PI;
        } else {
            // 顺时针方向，确保ang2 < ang1
            if (ang2 > ang1) ang2 -= 2*Math.PI;
        }

        // 生成圆弧上的点
        List<EastNorth> arc = new ArrayList<>();
        for (int i=0;i<=numPoints;i++){
            double tt = (double)i/numPoints;          // 插值参数 [0,1]
            double ang = ang1 + (ang2 - ang1) * tt;   // 当前角度
            double x = center[0] + R * Math.cos(ang); // 圆弧点X坐标
            double y = center[1] + R * Math.sin(ang); // 圆弧点Y坐标
            arc.add(new EastNorth(x, y));
        }
        return arc;
    }

    public static List<Node> buildSmoothPolyline(Way way, double radiusMeters, OsmDataLayer layer) {
        // 1. 获取道路的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());
        int nPts = nodes.size();
        if (nPts < 2) return null;  // 道路至少需要2个点

        // 2. 将所有节点转换为平面坐标
        List<EastNorth> en = new ArrayList<>();
        for (Node n : nodes) en.add(toEastNorth(n.getCoor()));

        // 3. 为每个拐角预计算圆角
        List<double[]> T1s = new ArrayList<>();  // 存储每个拐角的第一个切点
        List<double[]> T2s = new ArrayList<>();  // 存储每个拐角的第二个切点
        List<List<EastNorth>> arcs = new ArrayList<>();  // 存储每个拐角的圆弧

        for (int i=0;i<nPts-2;i++){
            EastNorth A = en.get(i);      // 前一个点
            EastNorth B = en.get(i+1);    // 拐角点
            EastNorth C = en.get(i+2);    // 后一个点

            List<EastNorth> arc = filletArcEN(A,B,C,radiusMeters, 20);  // 生成20个点的圆弧

            if (arc == null) {
                // 该拐角无法生成圆角（半径过大或角度问题）
                T1s.add(null);
                T2s.add(null);
                arcs.add(null);
            } else {
                // 存储切点和圆弧
                EastNorth t1 = arc.get(0), t2 = arc.get(arc.size()-1);
                T1s.add(new double[]{t1.getX(), t1.getY()});
                T2s.add(new double[]{t2.getX(), t2.getY()});
                arcs.add(arc);
            }
        }

        // 4. 构建最终的经纬度坐标序列
        List<LatLon> finalLatLons = new ArrayList<>();

        // 添加起始点（第一个节点）
        finalLatLons.add(toLatLon(en.get(0)));

        // 5. 遍历所有线段，用圆弧替换拐角
        for (int i=0;i<nPts-1;i++){
            boolean filletAtNext = (i <= nPts-3) && (arcs.get(i) != null);  // 检查下一个拐角是否有有效圆角

            if (filletAtNext){
                // 使用圆角路径
                double[] T1 = T1s.get(i);
                LatLon llT1 = toLatLon(new EastNorth(T1[0], T1[1]));

                // 添加第一个切点（如果与上个点不同）
                if (!finalLatLons.get(finalLatLons.size()-1).equals(llT1))
                    finalLatLons.add(llT1);

                // 添加圆弧上的所有点（跳过第一个点，避免重复）
                List<EastNorth> arc = arcs.get(i);
                for (int k=1;k<arc.size();k++)
                    finalLatLons.add(toLatLon(arc.get(k)));
            } else {
                // 无法生成圆角，使用原始路径点
                LatLon llNext = toLatLon(en.get(i+1));
                if (!finalLatLons.get(finalLatLons.size()-1).equals(llNext))
                    finalLatLons.add(llNext);
            }
        }

        // 6. 创建新的节点对象
        List<Node> newNodes = new ArrayList<>();
        for (LatLon ll : finalLatLons) {
            Node nn = new Node(ll);  // 创建新节点
            newNodes.add(nn);
        }
        return newNodes;
    }
}
