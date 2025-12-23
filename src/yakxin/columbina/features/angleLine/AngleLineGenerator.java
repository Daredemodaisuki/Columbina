package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.dto.outputs.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.List;

public final class AngleLineGenerator extends AbstractGenerator<AngleLineParams> {

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, AngleLineParams params)  // 内部注意类型检查！
    {
        if (input.quickPrecomputedData.get("nodeIndex") instanceof Integer)  // 如果有快捷传递中间量
            return buildAngleLine(
                    input.nodes.get(0), input.ways.get(0), (Integer) input.quickPrecomputedData.get("nodeIndex"),
                    params.angleDeg, params.surfaceLength
            );
        return buildAngleLine(
                input.nodes.get(0), input.ways.get(0), UtilsData.getNodeIndex(input.nodes.get(0), input.ways.get(0)),
                params.angleDeg, params.surfaceLength
        );
        // throw new ColumbinaException(I18n.tr("No or wrong precomputed data received."));
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
            Node node, Way way, int nodeIndex,
            double angleDeg, double surfaceLength  // 左拐角度为正，右拐为负
    ) {
        // 查找是第几个（不用查重了，action具体检查中已经做了）
        int nodeNum = way.isClosed() ? way.getNodes().size() - 1 : way.getNodes().size();
        List<Node> wayNodes = way.getNodes();

        /// 正式计算
        ColumbinaEN prevEN = new ColumbinaEN(wayNodes.get((nodeIndex + nodeNum - 1) % nodeNum).getEastNorth());
        ColumbinaEN startEN = new ColumbinaEN(wayNodes.get(nodeIndex).getEastNorth());

        double enterAngleRad = prevEN.deflectionRadTo(startEN);
        double turningAngleRad = Math.toRadians(angleDeg);
        double enLength = UtilsMath.surfaceDistanceToEastNorth(surfaceLength, node.lat());

        ColumbinaEN destination = startEN.walk(enterAngleRad + turningAngleRad, enLength);

        List<Node> newNodes = new ArrayList<>();
        newNodes.add(node);
        newNodes.add(new Node(destination));

        return new ColumbinaSingleOutput(newNodes, new ArrayList<>());  // 无failed部分
    }
}


