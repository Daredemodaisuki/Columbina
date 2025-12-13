package yakxin.columbina.abstractClasses.actionMiddle;

import org.openstreetmap.josm.command.AddCommand;
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
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

import java.util.*;

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
            // int minNodeSelection, int maxNodeSelection, int minWaySelection, int maxWaySelection
    ) {
        super(name, iconName, description, shortcut, generator, preference);
        // this.minNodeSelection = minNodeSelection;  // 这个模板下的操作没有批量的概念，暂时不要这些字段
        // this.maxNodeSelection = maxNodeSelection;
        // this.minWaySelection = minWaySelection;
        // this.maxWaySelection = maxWaySelection;
    }

    @Override
    public int checkInputNum(ColumbinaInput totalInput) {
        // 检查是否是输入一个节点+一条路径，不检查节点是否在路径上（由生成器内部判断）
        if (totalInput.getInputNum(Node.class) != 1)
            throw new IllegalArgumentException(I18n.tr("No node or multiple nodes are selected."));
        if (totalInput.getInputNum(Way.class) != 1)
            throw new IllegalArgumentException(I18n.tr("No way or multiple ways are selected."));
        // if (!inputs.getNodes().getFirst().getReferrers().contains(inputs.getWays().getFirst()))
        //     throw new IllegalArgumentException(I18n.tr("The way selected doesn''t contain the node selected."));
        return CHECK_OK;
    }

    // 暂时交由具体动作类实现
    public abstract int checkInputDetails(List<ColumbinaSingleInput> singleInputs);

    @Override
    public List<ColumbinaSingleInput> splitBatchInputs(ColumbinaInput inputs) {
        // 批量输入分包（这个模板不支持批量，所以直接将全部输入扔进去）
        return new ArrayList<>(Collections.singleton(new ColumbinaSingleInput(inputs)));
    }

    @Override
    public List<Command> concludeAddCommands(
            DataSet ds, List<ColumbinaSingleInput> input,
            boolean copyTag
    ) {
        if (input == null || input.isEmpty())
            throw new ColumbinaException(I18n.tr("Empty or null input for concludeAddCommands()."));

        List<Command> addCommands = new ArrayList<>();

        // 调用生成传入的函数计算路径
        ColumbinaSingleInput singleInput = input.getFirst();  // 这个模板不支持批量，所以直接getFirst
        ColumbinaSingleOutput singleOutput = generator.getOutputForSingleInput(singleInput, params);
        if (singleOutput == null) return null;
        if (!singleOutput.ifCanMakeAWay()) return null;

        // 画新线
        newWay = singleOutput.linkNodesToWay();

        // 复制原Way标签
        if (copyTag) {
            Map<String, String> keys = getNewWayTags(singleInput);
            if (newWay != null && keys != null)
                newWay.setKeys(keys);
        }

        // 正式构建绘制命令
        if (newWay != null) {
            for (Node n : singleOutput.newNodes.stream().distinct().toList()) {  // 路径内部可能有节点复用（如闭合线），去重
                if (!ds.containsNode(n))  // 新路径的节点在ds中未绘制（不是复用的）才准备绘制
                    addCommands.add(new AddCommand(ds, n));  // 添加节点到命令序列
            }
            addCommands.add(new AddCommand(ds, newWay));  // 添加线到命令序列
        }

        return addCommands;
    }

    @Override
    public List<Command> concludeRemoveCommands(DataSet ds) {
        return new ArrayList<Command>();  // 这个模板下不需要移除，返回空列表即可
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


