package yakxin.columbina.features.chamfer;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.List;

public final class ChamferGenerator extends AbstractGenerator<ChamferParams> {
    // 模式常量
    public static final int DISTANCE_MODE = 0;
    public static final int ANGLE_A_MODE = 1;

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, ChamferParams params) {
        if (input.ways != null && input.ways.size() == 1) {
            return buildChamferPolyline(
                    input.ways.get(0), params.mode,
                    params.surfaceDistanceA, params.surfaceDistanceC,
                    params.angleADeg
            );
        }
        return null;
    }

    // 绘制一个斜角
    public static ArrayList<EastNorth> getChamferCutNodesUsingDistance(
            ColumbinaCorner corner, double enDistanceA, double enDistanceC
    ) {
        if (corner.lenBA < UtilsMath.EPSILON_STRICT || corner.lenBC < UtilsMath.EPSILON_STRICT) return null;  // 检查向量有效性（不能太短近乎于点挤在一起）

        // 获取新切点
        ColumbinaEN c1 = corner.B.walk(corner.BA.bearingRad(), enDistanceA);  // BA侧切点
        ColumbinaEN c2 = corner.B.walk(corner.BC.bearingRad(), enDistanceC);  // BC侧切点

        // 检查切点长度是否足够
        if (enDistanceA >= corner.lenBA || enDistanceC >= corner.lenBC) return null;

        ArrayList<EastNorth> cutNodes = new ArrayList<>();
        cutNodes.add(c1.getEastNorth());
        cutNodes.add(c2.getEastNorth());

        return cutNodes;
    }
    public static ArrayList<EastNorth> getChamferCutNodesUsingAngleA(
            ColumbinaCorner corner, double enDistanceA, double angleADeg
    ) {
        if (corner.lenBA < UtilsMath.EPSILON_STRICT || corner.lenBC < UtilsMath.EPSILON_STRICT) return null;  // 检查向量有效性（不能太短近乎于点挤在一起）

        // 直接从corner获取拐点夹角
        double thetaB = corner.angleRad;  // 夹角（弧度[0, π]）

        double thetaA = Math.toRadians(angleADeg);  // A切角（弧度）
        if (thetaA + thetaB >= Math.PI - UtilsMath.EPSILON_STRICT) return null;  // 夹角B+切角A>180°，无法构成三角形
        // 计算enDistanceC：Bc1/sinC = Bc2/sinA → enDistanceC = Bc2=Bc1*sinA/sinC
        double enDistanceC = enDistanceA * Math.sin(thetaA) / Math.sin(Math.PI - thetaB - thetaA);

        // 切点
        ColumbinaEN c1 = corner.B.walk(corner.BA.bearingRad(), enDistanceA);  // BA侧切点
        ColumbinaEN c2 = corner.B.walk(corner.BC.bearingRad(), enDistanceC);  // BC侧切点
        if (enDistanceA >= corner.lenBA || enDistanceC >= corner.lenBC) return null;  // 检查切点长度是否足够

        ArrayList<EastNorth> cutNodes = new ArrayList<>();
        cutNodes.add(c1.getEastNorth());
        cutNodes.add(c2.getEastNorth());

        return cutNodes;
    }

    public static ColumbinaSingleOutput buildChamferPolyline(
            Way way, int mode,
            double surfaceDistanceA, double surfaceDistanceC,
            double angleADeg
    ) {
        List<Long> failedNodes = new ArrayList<>();

        // 获取路径的所有节点
        List<Node> nodes = new ArrayList<>(way.getNodes());
        boolean isClosed = way.isClosed();
        int numNode = isClosed ? nodes.size() - 1 : nodes.size();  // 实际节点数（去除闭合点）
        if (numNode < 3) return null;  // 路径至少需要3个点
        int numCorner = isClosed ? numNode : numNode - 2;  // 拐角数

        List<List<EastNorth>> chamfers = new ArrayList<>();  // 存储每个拐角的切点对

        // 为路径计算所有拐角
        for (int i = 0; i < numCorner; i++) {
            try {
                // 使用ColumbinaCorner创建拐角
                ColumbinaCorner corner = ColumbinaCorner.create(way, i);
                double latB = corner.latB;  // 拐点B的纬度

                // JOSM用Mercator投影的NorthEast坐标等角不等距，需要重算距离
                double enDistanceA1, enDistanceA2, enDistanceC;
                try {
                    enDistanceA1 = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceA, latB);
                    enDistanceA2 = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceA, UtilsMath.toLatLon(corner.A).lat());
                    enDistanceC = UtilsMath.surfaceDistanceToEastNorth(surfaceDistanceC, latB);
                } catch (ColumbinaException exSurToEn) {
                    // 如果纬度接近90度，直接失败跳过这个斜角
                    chamfers.add(null);
                    failedNodes.add(nodes.get(i + 1).getUniqueId());
                    continue;
                }

                // 有EN长度之后继续算切角
                List<EastNorth> chamfer;
                if (mode == DISTANCE_MODE) chamfer = getChamferCutNodesUsingDistance(corner, enDistanceA1, enDistanceC);
                else if (mode == ANGLE_A_MODE) chamfer = getChamferCutNodesUsingAngleA(corner, enDistanceA2, angleADeg);
                else chamfer = null;

                if (chamfer == null) {  // 该拐角没有生成斜角（半径过大或角度问题）
                    chamfers.add(null);
                    failedNodes.add(nodes.get(i + 1).getUniqueId());
                } else {
                    chamfers.add(chamfer);
                }
            } catch (ColumbinaException exCorner) {
                // 创建拐角失败
                chamfers.add(null);
                failedNodes.add(nodes.get(i + 1).getUniqueId());
            }
        }

        // 最终的经纬度坐标序列
        List<Node> finalNodes = new ArrayList<>();
        // 对于非闭合路径（或闭合点没有曲线的闭合路径），从第一个节点开始；
        // 对于闭合路径且闭合点有曲线，从第一条曲线第一个点开始（下面for中添加）
        if (!way.isClosed() || (way.isClosed() && chamfers.get(chamfers.size() - 1) == null))
            finalNodes.add(nodes.get(0));
        // 遍历所有拐角
        for (int i = 0; i < numCorner; i ++) {
            if (chamfers.get(i) != null) {  // 有过渡曲线就添加曲线上的所有点
                List<EastNorth> curve = chamfers.get(i);
                for (EastNorth eastNorth : curve)
                    finalNodes.add(new Node(UtilsMath.toLatLon(eastNorth)));
            } else finalNodes.add(nodes.get(i + 1));  // 没有过渡曲线，添加原始拐点
        }
        // 添加最后一个节点
        if (way.isClosed()) finalNodes.add(finalNodes.get(0));
        else finalNodes.add(nodes.get(nodes.size() - 1));

        return new ColumbinaSingleOutput(finalNodes, failedNodes);
        // 正式绘制前注意去重
    }
}


