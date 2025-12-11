package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.List;

public final class AngleLineGenerator extends AbstractGenerator<AngleLineParams> {

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, AngleLineParams params)  // 内部注意类型检查！
    {
        return buildAngleLine(
                input.nodes.getFirst(), input.ways.getFirst(),
                params.angleDeg, params.surfaceLength
        );
    }

    /**
     * 从某条路径上的某个节点引出一条线，使得其与路径上上一个节点到该节点的线段形成angleRad偏角
     * @param node 路径上的节点
     * @param way 路径
     * @param angleDeg 偏角
     * @param surfaceLength 画线长度
     * @return 绘制的线段
     */
    public static ColumbinaSingleOutput buildAngleLine(
            Node node, Way way,
            double angleDeg, double surfaceLength  // 左拐角度为正，右拐为负
    ) {
        /// 具体检查
        // 闭合曲线过滤闭合点，顺便查重
        int count = 0, nodeIndex = 0, nodeNum = way.isClosed() ? way.getNodes().size() - 1 : way.getNodes().size();
        List<Node> wayNodes = new ArrayList<>();
        for (int i = 0; i < nodeNum; i ++) {
            if (way.getNode(i) == node) {
                count ++; nodeIndex = i;
            }
            wayNodes.add(way.getNode(i));
        }

        // 检查路径是否包含节点
        if (count == 0)
            throw new ColumbinaException(I18n.tr("The way selected doesn''t contain the node selected."));
        // 检查路径是否多次包含该节点（自交路径的自交点）
        if (count > 1)
            throw new ColumbinaException(
                    I18n.tr("The node is the self-intersection point of a self-intersecting way. ")
                            + I18n.tr("The bearing angle into this node cannot be determined.")
            );

        // 检查路径是否是非闭合路径第一个点
        if (node == wayNodes.getFirst() && !way.isClosed())
            throw new ColumbinaException(
                    I18n.tr("The selected node is the first node of the way. ")
                            + I18n.tr("The bearing angle into this node cannot be determined.")
            );

        /// 正式计算
        EastNorth prevEN = wayNodes.get((nodeIndex + nodeNum - 1) % nodeNum).getEastNorth();
        EastNorth startEN = wayNodes.get(nodeIndex).getEastNorth();
        double[] prev = new double[]{prevEN.getX(), prevEN.getY()};
        double[] start = new double[]{startEN.getX(), startEN.getY()};

        double enterAngleRad = UtilsMath.getBearingRadFromAtoB(prev, start);
        double turningAngleRad = Math.toRadians(angleDeg);
        double enLength = UtilsMath.surfaceDistanceToEastNorth(surfaceLength, node.lat());

        double[] destination = UtilsMath.walkAlongAngleDistance(start, enterAngleRad + turningAngleRad, enLength);

        List<Node> newNodes = new ArrayList<>();
        newNodes.add(node);
        newNodes.add(new Node(new EastNorth(destination[0], destination[1])));

        return new ColumbinaSingleOutput(newNodes, new ArrayList<Long>());  // 无failed部分
    }
}


