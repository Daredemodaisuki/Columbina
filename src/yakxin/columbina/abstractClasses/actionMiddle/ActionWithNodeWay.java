package yakxin.columbina.abstractClasses.actionMiddle;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.abstractClasses.AbstractParams;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

import java.util.ArrayList;
import java.util.List;

public abstract class ActionWithNodeWay<
        GeneratorType extends AbstractGenerator<ParamType>,  // 生成器泛型
        PreferenceType extends AbstractPreference<ParamType>,  // 首选项泛型
        ParamType extends AbstractParams> // 输入参数泛型
        extends AbstractDrawingAction<GeneratorType, PreferenceType, ParamType>
{
    // private final int minNodeSelection;
    // private final int maxNodeSelection;
    // private final int minWaySelection;
    // private final int maxWaySelection;

    /**
     * 构造函数
     *
     * @param name             功能名称（I18n后）
     * @param iconName         菜单栏功能图标（I18n后）
     * @param description      功能描述（I18n后）
     * @param shortcut         快捷键
     * @param generator        生成器实例
     * @param preference       首选项实例
     */
    public ActionWithNodeWay(
            String name, String iconName, String description, Shortcut shortcut,
            GeneratorType generator, PreferenceType preference
            // int minNodeSelection, int maxNodeSelection, int minWaySelection, int maxWaySelection
    ) {
        super(name, iconName, description, shortcut, generator, preference);
        // this.minNodeSelection = minNodeSelection;  // 这个模板下的操作没有批量的概念，暂时不要这些字段
        // this.maxNodeSelection = maxNodeSelection;
        // this.minWaySelection = minWaySelection;
        // this.maxWaySelection = maxWaySelection;
    }

    @Override
    public int checkInputNum(ColumbinaInput inputs) {
        // 检查是否是输入一个节点+一条路径，且节点在路径上
        if (inputs.getInputNum(Node.class) != 1)
            throw new IllegalArgumentException(I18n.tr("No node or multiple nodes are selected."));
        if (inputs.getInputNum(Way.class) != 1)
            throw new IllegalArgumentException(I18n.tr("No way or multiple ways are selected."));
        if (!inputs.getNodes().getFirst().getReferrers().contains(inputs.getWays().getFirst()))
            throw new IllegalArgumentException(I18n.tr("The way selected doesn''t contain the node selected."));
        return CHECK_OK;
    }

    @Override
    public List<Command> concludeAddCommands(
            DataSet ds, ColumbinaInput input,
            boolean copyTag
    ) {
        List<Command> commands = new ArrayList<>();
        // 批量输入分包（这个模板不支持批量，所以直接将全部输入扔进去）
        ColumbinaSingleInput singleInput = new ColumbinaSingleInput(input);
        // 调用生成传入的函数计算路径
        ColumbinaSingleOutput singleOutput = generator.getNewNodeWayForSingleInput(singleInput, params);

        return new ArrayList<>();
    }

    @Override
    public List<Command> concludeRemoveCommands(DataSet ds) {
        return List.of();
    }

    @Override
    public List<OsmPrimitive> getWhatToSelectAfterDraw() {
        return new ArrayList<>();
    }
}


