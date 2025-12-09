package yakxin.columbina.abstractClasses;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.ColumbinaSeqCommand;
import yakxin.columbina.data.inputs.ColumbinaInput;
import yakxin.columbina.utils.UtilsUI;

import java.awt.event.ActionEvent;
import java.util.*;

/**
 * 总的功能操作逻辑：根据输入的节点或者路径绘制新路径
 * <p>不管具体的参数、生成方式是什么了，只管通用逻辑，一切差异化的东西在具体的action类实现，具体的参数、生成方式分别在各自类实现之后传实体进来。
 * <p>通用的逻辑是：获取当前选择输入（目前支持Way或Node）；弹出参数设置窗口，获取参数；对每个输入Way或Node使用生成器绘制新路径，并记录输入输出对；
 * 对输入中处理失败的节点提示；移除旧路径（目前只考虑输入Way可能需要移除，节点有需求再扩充）
 * @param <GeneratorType> 具体的生成器类
 * @param <PreferenceType> 具体的首选项类
 * @param <ParamType> 具体的参数类
 */
public abstract class AbstractDrawingAction <
        GeneratorType extends AbstractGenerator<ParamType>,  // 生成器泛型
        PreferenceType extends AbstractPreference<ParamType>,  // 首选项泛型
        ParamType extends AbstractParams>  // 输入参数泛型
        extends JosmAction
    {
    public static final int NO_LIMITATION_ON_INPUT_NUM = -1;
    public static final int CHECK_OK = 0;
    public static final int CHECK_OK_BUT_WARN = 1;
    public static final int USER_CANCEL = 2;

    /// 具体的参数、生成方式
    protected final GeneratorType generator;
    protected final PreferenceType preference;  // preference是final但是不影响它内部自己变
    protected ParamType params;  // params将在执行点击事件时具体获取，每次获取可能不一致

    /// 所有需要由具体action类定义的东西
    /**
     * 获取撤销重做栈的说明
     * @param inputs 输入要素
     * @param params 输入参数
     * @return 说明文本（I18n后）
     */
    public abstract String getUndoRedoInfo(ColumbinaInput inputs, ParamType params);

    /**
     * 将输入要素全部传入generator获取并汇总全部添加指令
     * <p>对于允许批量操作的操作类，需要将全部输入要素拆包（拆为一组一组的，每组输入产生一条路径），在for中获取单组输入产生的指令并合并到一起
     * <p>如果一组输入中有部分失败，在这个函数中直接弹窗警告，不影响全流程；
     * <p>如果一组输入完全失败，则抛出异常；如果是批量操作中某组完全失败，在for中抛出异常以不影响其他组输入。
     * <p>如有必要，这个函数应该在中间层或具体操作类中自行储存输入与输出的对应关系，以便concludeRemoveCommands使用。
     * <p>如果不需要添加，返回空的列表。
     * <p>如果全部都处理失败而没有添加指令，返回null。
     * @param ds 数据集
     * @param input 选定的旧路径
     * @param copyTag 是否拷贝标签
     * @return 对于全部输入产生的添加指令列表
     */
    public abstract List<Command> concludeAddCommands(
            DataSet ds, ColumbinaInput input,
            boolean copyTag
    );

    /**
     * 汇总全部移除指令
     * <p>通常是移除旧的输入要素，如有此必要，应让concludeAddCommands在中间层或具体操作类中自行储存输入与输出的对应关系，以便此处对照移除。
     * <p>如果有无法移除的情况，则不要移除该要素，直接弹窗警告，不影响全流程。
     * <p>如果不需要移除，返回空的列表。
     * @param ds 数据集
     * @return 对于全部输入产生的移除指令列表
     */
    public abstract List<Command> concludeRemoveCommands(DataSet ds);

    /**
     * 检查输入要素的数量是否合法
     * <p>具体的数量要求应在中间层或具体操作类中定义。
     * @param inputs 输入要素
     * @return 检查结果状态
     */
    public abstract int checkInputNum(ColumbinaInput inputs);

    /**
     * 获取需要选中的新路径
     * <p>如有必要，根据类内部记录的输入输出对（由concludeAddCommands负责）返回绘图后需要选择的对象；
     * <p>如无必要，返回空列表。
     * @return 需要选择的对象列表
     */
    public abstract List<OsmPrimitive> getWhatToSelectAfterDraw();

    /**
     * 获取新绘制路径所需的标签，为getAddCmd所用
     * @param singleInput 单组输入要素
     * @return 标签映射
     */
    public abstract Map<String, String> getNewWayTags(Object singleInput);

    // 上述之外，每种Action所需的名字、图标等是固定的，为了简便、不在Columbina主类写太多参数，
    // 每个action可以写一个静态的create函数返回new自身（静态工厂），但是貌似语法不支持在这里限制必须实现一个abstract static，
    // 这里需要自行注意一下，且如果写了这个函数，action的构造函数可以改为private
    // 或者不嫌麻烦就在Columbina填一大堆也行。


    /**
     * 构造函数
     * @param name 功能名称（I18n后）
     * @param iconName 菜单栏功能图标（I18n后）
     * @param description 功能描述（I18n后）
     * @param shortcut 快捷键
     * @param generator 生成器实例
     * @param preference 首选项实例
     */
    public AbstractDrawingAction(
            String name, String iconName, String description, Shortcut shortcut,
            GeneratorType generator, PreferenceType preference
    ) {
        // 调用父类构造函数设置动作属性
        super(
                name,  // 菜单显示文本
                iconName,  // 图标
                description,  // 工具提示
                shortcut,  // 快捷键
                true,  // 启用工具栏按钮
                false
        );
        this.generator = generator;
        this.preference = preference;
        // params将在执行点击事件时具体获取，每次获取可能不一致
        // this.inputFeatureType = inputFeatureType;  // 弃用
        // this.minSelection = minSelection;  // 下放到中间层
        // this.maxSelection = maxSelection;
    }

    /**
     * 点击事件：绘制数据通用动作模板
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer layer; DataSet dataSet; ColumbinaInput input;
        boolean deleteOld; boolean selectNew; boolean copyTag;

        // 检查
        try {
            input = new ColumbinaInput();
            layer = input.getLayer();
            dataSet = input.getDataSet();

            // 数量检查（不可接受的数量将在checkInputNum抛IllegalArgumentException）
            if (checkInputNum(input) == USER_CANCEL) return;  // 用户取消时直接退出

            // 弹出参数设置窗口，获取参数
            final ParamType params = preference.getParamsAndUpdatePreference();  // 重构后preference负责弹窗，本来也就是设置preference的窗口
            if (params == null) return;  // 用户取消操作
            // 存入类成员以便concludeAddCommands之UtilsData.getAddCmd(…, params, …)
            // 和下面的ColumbinaSeqCommand(getUndoRedoInfo(…, params), …);使用
            this.params = params;
            // 获取影响整个动作模板逻辑的关键通用参数
            deleteOld = params.deleteOld;
            selectNew = params.selectNew;
            copyTag = params.copyTag;
        } catch (ColumbinaException | IllegalArgumentException exCheck) {
            UtilsUI.errorInfo(exCheck.getMessage());
            return;
        }

        // 绘制新路径
        List<Command> cmdsAdd;
        try {
            cmdsAdd = concludeAddCommands(dataSet, input, copyTag);
            if (cmdsAdd == null)
                throw new ColumbinaException(I18n.tr("No input was successfully processed."));
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            UtilsUI.errorInfo(exAdd.getMessage());
            return;
        }

        // 绘制部分的撤销重做栈处理并正式提交执行
        if (!cmdsAdd.isEmpty()) {
            Command cmdAdd = new ColumbinaSeqCommand(getUndoRedoInfo(input, params), cmdsAdd, "RoundCorners");
            UndoRedoHandler.getInstance().add(cmdAdd);
        }

        // 移除旧路径（如果需要的话）
        if (deleteOld) {
            try {
                List<Command> cmdsRmv = concludeRemoveCommands(dataSet);
                if (!cmdsRmv.isEmpty()) {  // 如果全部都没有删除/替换，cmdsRmv为空会错错爆;
                    Command cmdRmv = new ColumbinaSeqCommand(I18n.tr("Columbina: Remove original ways"), cmdsRmv, "RemoveOldWays");
                    UndoRedoHandler.getInstance().add(cmdRmv);
                }
            } catch (ColumbinaException | IllegalArgumentException | ReplaceGeometryException exRemove) {
                UtilsUI.warnInfo(exRemove.getMessage());
                // 一个不能换不影响尝试换其他的
            }
        }

        // 选中新路径
        if (selectNew) {
            List<OsmPrimitive> whatToSelectAfterDraw = getWhatToSelectAfterDraw();
            if (!whatToSelectAfterDraw.isEmpty()) dataSet.setSelected(whatToSelectAfterDraw);
        }
    }
}


