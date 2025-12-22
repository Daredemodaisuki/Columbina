package yakxin.columbina.abstractClasses.actionMiddle;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.abstractClasses.AbstractParams;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.outputs.ColumbinaOutputIntent;
import yakxin.columbina.data.dto.outputs.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

import java.util.*;

public abstract class ActionWithNodeWay<
        GeneratorType extends AbstractGenerator<ParamType>,  // 生成器泛型
        PreferenceType extends AbstractPreference<ParamType>,  // 首选项泛型
        ParamType extends AbstractParams> // 输入参数泛型
        extends AbstractDrawingAction<GeneratorType, PreferenceType, ParamType>
{
    private Way newWay = null;

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
    ) {
        super(name, iconName, description, shortcut, generator, preference);
    }

    // 暂时交由具体动作类实现
    public abstract int checkInputNum(ColumbinaInput totalInput);

    public abstract int checkInputDetails(List<ColumbinaSingleInput> singleInputs);

    @Override
    public List<ColumbinaSingleInput> splitBatchInputs(ColumbinaInput totalInput) {
        // 批量输入分包（这个模板不支持批量，所以直接将全部输入扔进去）
        return new ArrayList<>(Collections.singleton(new ColumbinaSingleInput(totalInput)));
    }

    @Override
    public List<Command> concludeAddCommands(
            DataSet ds, List<ColumbinaSingleInput> input,
            boolean copyTag
    ) {
        if (input == null || input.isEmpty())
            throw new ColumbinaException(I18n.tr("Empty or null input for concludeAddCommands()."));
        
        // 调用生成传入的函数计算路径
        ColumbinaSingleInput singleInput = input.get(0);  // 这个模板不支持批量，所以直接getFirst
        ColumbinaSingleOutput singleOutput = generator.getOutputForSingleInput(singleInput, params);
        if (singleOutput == null) return null;
        if (!singleOutput.isValid()) return null;
        
        // 收集新线（目前假定只输出一条新线）
        newWay = (Way) singleOutput.representatives.get(0);
        
        // 复制原Way标签
        if (copyTag) {
            Map<String, String> wayTags = getNewWayTags(singleInput);
            newWay.setKeys(wayTags);
        }
        
        // 转为指令
        List<Command> commands = new ArrayList<>(ColumbinaOutputIntent.toCommands(singleOutput.outputIntents, ds));
        // for (ColumbinaOutputIntent<?> intent : singleOutput.outputIntents) commands.addAll(intent.resolveToCommand(ds));
        
        if (commands.isEmpty())  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));
        
        // 去重防止提交重复添加（ColumbinaOutputIntent.toCommands已去重）
        // commands = commands.stream().distinct().collect(Collectors.toList());
        
        return commands;
    }

    @Override
    public List<Command> concludeRemoveCommands(DataSet ds) {
        return new ArrayList<>();  // 这个模板下不需要移除，返回空列表即可
    }

    @Override
    public List<OsmPrimitive> getWhatToSelectAfterDraw() {
        if (newWay != null)
            return new ArrayList<>(Collections.singleton(newWay));
        else return null;
    }

    @Override
    public Map<String, String> getNewWayTags(ColumbinaSingleInput singleInput) {
        return new HashMap<>();  // 暂无需新标签
    }
}


