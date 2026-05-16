package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsData;

import java.awt.event.KeyEvent;
import java.util.List;

public final class AngleLineAction extends
        AbstractDrawingAction<AngleLineGenerator, AngleLinePreference, AngleLineParams>
{

    /**
     * 构造函数
     *
     * @param name        功能名称（I18n后）
     * @param iconName    菜单栏功能图标（I18n后）
     * @param description 功能描述（I18n后）
     * @param shortcut    快捷键
     * @param generator   生成器实例
     * @param preference  首选项实例
     */
    public AngleLineAction(String name, String iconName, String description, Shortcut shortcut, AngleLineGenerator generator, AngleLinePreference preference) {
        super(name, iconName, description, shortcut, generator, preference);
    }

    /**
     * 懒得在主类填参数就用静态工厂方法
     * @return 构建好的实例
     */
    public static AngleLineAction create() {
        return new AngleLineAction(
                I18n.tr("Oriented Line"), "OrientedLine",
                I18n.tr("Make a line started on the selected node with a specified distance and deflection angle."),
                Shortcut.registerShortcut(
                        "tools:orientedLine",
                        "More tools: Columbina/Oriented line",
                        KeyEvent.VK_F,
                        Shortcut.ALT_CTRL_SHIFT
                ),
                new AngleLineGenerator(),
                new AngleLinePreference()
        );
    }

    @Override
    public String getUndoRedoInfo(ColumbinaInput inputs, AngleLineParams params) {
        return I18n.tr("Make oriented line: from node {0}, {1} {2}°, {3}m",
                inputs.getWays().get(0).getUniqueId(),
                params.angleDeg < 0 ? I18n.tr("left") : I18n.tr("right"),
                params.angleDeg,
                params.surfaceLength);
    }

    @Override
    public int checkInputNum(ColumbinaInput totalInput) {
        // 检查是否是输入一个节点+一条路径，不检查节点是否在路径上（由checkInputDetails判断）
        UtilsData.checkInputNum(totalInput, 1, 1, 1, 1);
        return CHECK_OK;
    }

    @Override
    public int checkInputDetails(List<ColumbinaSingleInput> singleInputs) {
        // 这个操作不支持批量，前面经过了数量检查，所以只有一组输入
        ColumbinaSingleInput singleInput = singleInputs.get(0);
        Way way = singleInput.ways.get(0);
        Node node = singleInput.nodes.get(0);

        // 检查节点是否在路径上且不是自交节点
        int nodeIndex = UtilsData.getNodeIndex(node, way);
        switch (nodeIndex) {
            case UtilsData.NODE_NOT_FOUND:  // 节点不在路径上
                throw new ColumbinaException(I18n.tr("The way selected doesn''t contain the node selected."));
            case UtilsData.SELF_INTERSECTION:  // 自交路径的自交节点
                throw new ColumbinaException(
                        I18n.tr("The node is the self-intersection point of a self-intersecting way. ")
                                + I18n.tr("The bearing angle into this node cannot be determined.")
                );
            case 0:  // 非闭合路径第一个点
                if (!way.isClosed())
                    throw new ColumbinaException(
                        I18n.tr("The selected node is the first node of the way. ")
                                + I18n.tr("The bearing angle into this node cannot be determined.")
                    );
        }

        // 快捷传递中间量
        singleInputs.get(0).quickPrecomputedData.put("nodeIndex", nodeIndex);

        return CHECK_OK;
    }

    @Override
    public List<ColumbinaSingleInput> splitBatchInputs(ColumbinaInput totalInput) {
        return defaultNonBatchSplitInputs(totalInput);
    }
}
