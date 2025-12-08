package yakxin.columbina.features.chamfer;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.DrawingNewNodeResult;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.List;

public final class ChamferGenerator extends AbstractGenerator<ChamferParams, Way> {

    public static final int USING_DISTANCE = 0;
    public static final int USING_ANGLE_A = 1;

    @Override
    public DrawingNewNodeResult getNewNodeWayForSingleInput(Way input, ChamferParams params) {
        return buildChamferPolyline(
                input, params.mode,
                params.surfaceDistanceA, params.surfaceDistanceC,
                params.angleADeg
        );
    }

    // 绘制一个斜角
    // TODO:检查距离是否足以生成斜角
    public static ArrayList<EastNorth> getChamferCutNodesUsingDistance(
            EastNorth A, EastNorth B, EastNorth C,
            double enDistanceA, double enDistanceC
    ) {
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

        // 获取新切点
        double[] c1 = UtilsMath.add(b, UtilsMath.mul(u1, enDistanceA));  // BA侧切点
        double[] c2 = UtilsMath.add(b, UtilsMath.mul(u2, enDistanceC));  // BC侧切点

        // 检查切点长度是否足够
        if (enDistanceA >= n1 || enDistanceC >= n2) return null;

        ArrayList<EastNorth> cutNodes = new ArrayList<>();
        cutNodes.add(new EastNorth(c1[0], c1[1]));
        cutNodes.add(new EastNorth(c2[0], c2[1]));

        return cutNodes;
    }
    public static ArrayList<EastNorth> getChamferCutNodesUsingAngleA(
            EastNorth A, EastNorth B, EastNorth C,
            double enDistanceA, double angleADeg
    ) {
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

        // 拐点夹角（方向向量点积取acos）
        double dp = Math.max(-1.0, Math.min(1.0, UtilsMath.dot(u1, u2)));  // 点积（限制在[-1,1]范围内）
        double thetaB = Math.acos(dp);  // 夹角（弧度[0,π]）

        double thetaA = Math.toRadians(angleADeg);  // A切角（弧度）
        if (thetaA + thetaB >= Math.PI - 1e-9) return null;  // 夹角B+切角A>180°，无法构成三角形

        double[] c1 = UtilsMath.add(b, UtilsMath.mul(u1, enDistanceA));  // BA侧切点
        // 计算enDistanceC：Bc1/sinC = Bc2/sinA → enDistanceC = Bc2=Bc1*sinA/sinC （直接en距离算吧）
        double enDistanceC = enDistanceA * Math.sin(thetaA) / Math.sin(Math.PI - thetaB - thetaA);
        double[] c2 = UtilsMath.add(b, UtilsMath.mul(u2, enDistanceC));

        // 检查切点长度是否足够
        if (enDistanceA >= n1 || enDistanceC >= n2) return null;

        ArrayList<EastNorth> cutNodes = new ArrayList<>();
        cutNodes.add(new EastNorth(c1[0], c1[1]));
        cutNodes.add(new EastNorth(c2[0], c2[1]));

        return cutNodes;
    }

    public static DrawingNewNodeResult buildChamferPolyline(
            Way way, int mode,
            double surfaceDistanceA, double surfaceDistanceC,
            double angleADeg
    ) {
        List<Long> failedNodes = new ArrayList<>();
        // 获取路径的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());
        int nPts = nodes.size();
        if (nPts < 3) return null;  // 路径至少需要3个点

        // 将所有节点转换为平面坐标
        List<EastNorth> en = new ArrayList<>();
        for (Node n : nodes) en.add(UtilsMath.toEastNorth(n.getCoor()));

        List<List<EastNorth>> chamfers = new ArrayList<>();  // 存储每个拐角的圆弧
        // 为每个拐角算切角
        for (int i = 0; i < nPts - 2; i ++) {
            EastNorth A = en.get(i);      // 起点    B → C
            EastNorth B = en.get(i + 1);  // 拐点    ↑
            EastNorth C = en.get(i + 2);  // 终点    A

            // JOSM用Mercator投影的NorthEast坐标等角不等距，需要重算距离，以拐点B取维度计算
            double latA = nodes.get(i).getCoor().lat();
            double latB = nodes.get(i + 1).getCoor().lat();
            double enDistanceA1, enDistanceA2, enDistanceC;
            try {
                enDistanceA1 = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceA, latB);
                enDistanceA2 = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceA, latA);
                enDistanceC = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceC, latB);
            } catch (ColumbinaException exSurToEn) {
                // 如果纬度接近90度，使用一个很小的正数，避免除0，但这样不准确，所以直接失败跳过这个斜角吧
                chamfers.add(null);
                failedNodes.add(nodes.get(i).getUniqueId());
                continue;
            }

            // 有EN长度之后继续算切角
            List<EastNorth> chamfer;
            if (mode == USING_DISTANCE) chamfer = getChamferCutNodesUsingDistance(A, B, C, enDistanceA1, enDistanceC);
            else if (mode == USING_ANGLE_A) chamfer = getChamferCutNodesUsingAngleA(A, B, C, enDistanceA2, angleADeg);
            else chamfer = null;

            if(chamfer == null) {  // 该拐角没有生成圆角（半径过大或角度问题）
                chamfers.add(null);
                failedNodes.add(nodes.get(i).getUniqueId());
            } else {
                chamfers.add(chamfer);
            }
        }
        if (way.isClosed()) {  // 闭合曲线首尾相连的首末点斜角
            double latB = nodes.getFirst().getCoor().lat();
            try {
                double enDistanceA = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceA, latB);
                double enDistanceC = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceC, latB);
                List<EastNorth> chamfer = getChamferCutNodesUsingDistance(
                        en.get(nPts - 2), en.get(0), en.get(1),
                        enDistanceA, enDistanceC
                );
                if(chamfer == null) {  // 该拐角没有生成圆角（半径过大或角度问题）
                    chamfers.add(null);
                    failedNodes.add(nodes.getFirst().getUniqueId());
                } else {
                    chamfers.add(chamfer);
                }
            } catch (ColumbinaException exSurToEN) {
                chamfers.add(null);
                failedNodes.add(nodes.getFirst().getUniqueId());
            }
        }

        // 最终的经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        // 添加起始点：如果原路径闭合，且首末点有斜角，则以首末点斜角（chamfer）终点为整个新路径起点；否则使用原路径第一个节点
        boolean useLastArcLastNode = way.isClosed() && chamfers.getLast() != null;
        if (useLastArcLastNode) finalNodes.add(new Node(UtilsMath.toLatLon(chamfers.getLast().getLast())));
        else finalNodes.add(way.getNode(0));
        // 遍历除最后一个节点以外所有路径节点，用圆弧替换拐角
        for (int i = 0; i < nPts - 2; i ++) {
            if (chamfers.get(i) != null) {  // 检查本次的拐角B（i+1）是否有有效圆角（圆角编号=A编号=i），圆弧存在则使用圆角路径
//                double[] T1 = chamfers.get(i).getFirst();
                Node chamferFirst = new Node(UtilsMath.toLatLon(chamfers.get(i).getFirst()));
                // 添加第一个切点c1（如果与上个点不同）
                if (!finalNodes.getLast().equals(chamferFirst)) finalNodes.add(chamferFirst);
                // 添加第二个切点c2
                finalNodes.add(new Node(UtilsMath.toLatLon(chamfers.get(i).getLast())));
            } else {  // 圆弧不存在则直接将拐点B加进来
                finalNodes.add(way.getNode(i + 1));
            }
        }
        // 终点处理
        if (way.isClosed()) {
            if (chamfers.getLast() != null) {
                finalNodes.add(new Node(UtilsMath.toLatLon(chamfers.getLast().getFirst())));
                finalNodes.add(new Node(UtilsMath.toLatLon(chamfers.getLast().getLast())));
            } else finalNodes.add(way.getNode(0));
        } else {
            finalNodes.add(way.getNode(way.getNodesCount() - 1));
        }

        return new DrawingNewNodeResult(finalNodes, failedNodes);
        // 正式绘制前注意去重
    }
}


