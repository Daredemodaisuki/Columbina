package yakxin.columbina.abstractClasses.actionMiddle;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.abstractClasses.AbstractParams;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.inputs.ColumbinaInput;

import java.util.ArrayList;
import java.util.List;

public abstract class ActionWithNodeWay<
        GeneratorType extends AbstractGenerator<ParamType>,  // 生成器泛型
        PreferenceType extends AbstractPreference<ParamType>,  // 首选项泛型
        ParamType extends AbstractParams> // 输入参数泛型
        extends AbstractDrawingAction<GeneratorType, PreferenceType, ParamType>
{
    private final int minSelection;
    private final int maxSelection;

    /**
     * 构造函数
     *
     * @param name             功能名称（I18n后）
     * @param iconName         菜单栏功能图标（I18n后）
     * @param description      功能描述（I18n后）
     * @param shortcut         快捷键
     * @param generator        生成器实例
     * @param preference       首选项实例
     * @param minSelection     最小选择数
     * @param maxSelection     最大选择数
     */
    public ActionWithNodeWay(
            String name, String iconName, String description, Shortcut shortcut,
            GeneratorType generator, PreferenceType preference,
            int minSelection, int maxSelection
    ) {
        super(name, iconName, description, shortcut, generator, preference);
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
    }

    @Override
    public List<Command> concludeAddCommands(
            DataSet ds, ColumbinaInput input,
            boolean copyTag
    ) {
        return new ArrayList<>();
    }

    @Override
    public List<Command> concludeRemoveCommands(DataSet ds) {
        return List.of();
    }

    @Override
    public int checkInputNum(ColumbinaInput inputs) {
        return CHECK_OK;
    }

    @Override
    public List<OsmPrimitive> getWhatToSelectAfterDraw() {
        return new ArrayList<>();
    }
}


