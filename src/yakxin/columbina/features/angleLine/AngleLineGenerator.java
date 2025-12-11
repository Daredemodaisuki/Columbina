package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

import java.util.ArrayList;
import java.util.List;

public final class AngleLineGenerator extends AbstractGenerator<AngleLineParams> {

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, AngleLineParams params)  // 内部注意类型检查！
    {
        return null;
    }

    /**
     * 从某条路径上的某个节点引出一条线，使得其与路径上上一个节点到该节点的线段形成angleRad偏角
     * @param node 路径上的节点
     * @param way 路径
     * @param angleRad 偏角
     * @param surfaceLength 画线长度
     * @return
     */
    public static ColumbinaSingleOutput buildAngleLine(
            Node node, Way way,
            double angleRad, double surfaceLength  // 左拐角度为正
    ) {
        /// 具体检查
        // 闭合曲线过滤闭合点
        List<Node> wayNodes = new ArrayList<>();
        for (int i = 0; i < (way.isClosed() ? way.getNodes().size() - 1 : way.getNodes().size()); i ++) {
            wayNodes.add(way.getNode(i));
        }
        // 查重
        int count = 0;
        for (Node n : wayNodes) {
            if (n == node) count ++;
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
        // TODO
        return null;
    }
}
