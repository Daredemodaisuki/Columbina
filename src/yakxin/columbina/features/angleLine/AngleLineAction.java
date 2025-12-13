package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.actionMiddle.ActionWithNodeWay;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public final class AngleLineAction extends
        ActionWithNodeWay<AngleLineGenerator, AngleLinePreference, AngleLineParams>
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
        super(
                name, iconName, description, shortcut,
                generator, preference
        );
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
                inputs.getWays().getFirst().getUniqueId(),
                params.angleDeg < 0 ? I18n.tr("left") : I18n.tr("right"),
                params.angleDeg,
                params.surfaceLength);
    }

    @Override
    public int checkInputDetails(List<ColumbinaSingleInput> singleInputs) {
        // 这个操作不支持批量，前面经过了数量检查，所以只有一组输入
        ColumbinaSingleInput singleInput = singleInputs.getFirst();
        Way way = singleInput.ways.getFirst();
        Node node = singleInput.nodes.getFirst();
        // 闭合曲线过滤闭合点，顺便查重
        int count = 0, nodeNum = way.isClosed() ? way.getNodes().size() - 1 : way.getNodes().size();
        List<Node> wayNodes = new ArrayList<>();
        for (int i = 0; i < nodeNum; i ++) {
            if (way.getNode(i) == node) {
                count ++;
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

        return CHECK_OK;
    }
}


