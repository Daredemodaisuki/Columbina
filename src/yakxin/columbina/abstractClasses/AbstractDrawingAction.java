package yakxin.columbina.abstractClasses;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.ColumbinaSeqCommand;
import yakxin.columbina.data.dto.AddCommandsCollected;
import yakxin.columbina.data.dto.LayerDatasetAndFeatureSelected;
import yakxin.columbina.data.dto.NewNodeWayCommands;
import yakxin.columbina.utils.UtilsData;
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
public abstract class AbstractDrawingAction
        <GeneratorType extends AbstractGenerator<ParamType, InputFeatureType>,  // 生成器泛型
                PreferenceType extends AbstractPreference<ParamType>,  // 首选项泛型
                ParamType extends AbstractParams,  // 输入参数泛型
                InputFeatureType extends OsmPrimitive>  // 输入要素泛型
        extends JosmAction
{
    // 目前（及计划中的）输出要素类型均为线（所以这个类叫「抽象画*路径*类」），暂时不考虑扩展其他输出类型，
    // 如果未来输入输出都灵活可变，可能需要继续添加一个输出要素类型泛型进一步处理AddCommandsCollected、concludeRemoveCommands和各种输入输出对
    // 以及可能需要重新讨论处理失败节点、移除旧路径的逻辑（约30行）是否仍然通用，如果不通用，需要修改逻辑或移出到各自的具体action实现中
    // （不管什么绘制功能，获取输入、调用生成器计算、获取结果并产生绘制指令、绘制这些步骤一定是必须的，目前这里约有50行代码〔还没算俩conclude方法，20+40〕，所以届时仍需要模板，不然抄改挺累）
    // 不过现在先不考虑，记在这里
    // TODO：对于输入是Node的情况，失败节点的列表可能没有意义（因为Node本身就是一个点，没有「拐角」）。
    //  可能需要重新考虑失败节点的处理逻辑，比如目前在concludeAddCommands中仍然保留了failedNodeIds，对于Node输入，failedNodeIds列表应该为空
    //  也可能需要调整getAddCmd或面向Node的具体生成器中的内容。
    public static final int NO_LIMITATION_ON_INPUT_NUM = -1;

    /// 选择限制
    private final Class<InputFeatureType> inputFeatureType;
    private final int minSelection;
    private final int maxSelection;

    /// 具体的参数、生成方式
    private final GeneratorType generator;
    private final PreferenceType preference;  // preference是final但是不影响它内部自己变
    private ParamType params;  // params将在执行点击事件时具体获取，每次获取可能不一致

    /// 所有需要由具体action类定义的东西
    public abstract String getUndoRedoInfo(List<InputFeatureType> selections, ParamType params);
    // 每种Action所需的名字、图标等是固定的，为了简便、不在Columbina主类写太多参数，
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
     * @param inputFeatureType 输入要素类型（有的操作是Node有的是Way）
     */
    public AbstractDrawingAction(
            String name, String iconName, String description, Shortcut shortcut,
            GeneratorType generator, PreferenceType preference, Class<InputFeatureType> inputFeatureType,
            int minSelection, int maxSelection
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
        this.inputFeatureType = inputFeatureType;
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
    }

    /**
     * 通过传入的generator汇总全部添加指令
     * @param ds 数据库
     * @param selections 选定的旧路径
     * @param copyTag 是否拷贝标签
     * @return 添加所需的指令、新旧路径对、失败的节点的打包
     */
    public AddCommandsCollected<InputFeatureType> concludeAddCommands(
            DataSet ds, List<InputFeatureType> selections,
            boolean copyTag
    ) {
        List<Command> commands = new ArrayList<>();
        Map<InputFeatureType, Way> inputOutputPairs = new HashMap<>();  // 输入节点/路径k与新绘制的路径v的打包对
        Map<InputFeatureType, List<Long>> failedNodeIds = new HashMap<>();  // 处理输入节点/路径k与处理时k上失败的节点v的打包对

        // 处理路径
        for (InputFeatureType featureToBeProcessed : selections) {  // 分别处理每个输入节点/路径
            try {  // 一条路径出错尽可能不影响其他的
                NewNodeWayCommands newNWCmd = UtilsData.getAddCmd(ds, featureToBeProcessed, generator, params, copyTag);  // 获取路径

                if (newNWCmd != null) {  // 应该不会和已提交但未执行（进入ds）的重复提交
                    commands.addAll(newNWCmd.addCommands);
                    // 新旧路径对、某条路径上的某个失败节点是对输入为路径的action来说的，如果本来就是输入一个起点，则没必要
                    // TODO:日后可以考虑重构NewNodeWayCommands使得这里关系更清晰一些？
                    inputOutputPairs.put(featureToBeProcessed, newNWCmd.newWay);
                    failedNodeIds.put(featureToBeProcessed, newNWCmd.failedNodeIds);
                }
                // TODO：修改硬编码
                else UtilsUI.warnInfo(I18n.tr(
                        "Algorithm did not return at least 2 nodes to form a way for way {0}, this way was not processed.",
                        featureToBeProcessed.getUniqueId()
                ));
            } catch (Exception exAdd) {
                // TODO：修改硬编码
                UtilsUI.errorInfo(I18n.tr("Unexpected error occurred while processing way {0}: {1}",
                        featureToBeProcessed.getUniqueId(), exAdd.getMessage()
                ));
            }
        }

        if (commands.isEmpty()) {  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));
        }
        // 去重防止提交重复添加
        commands = commands.stream().distinct().toList();

        return new AddCommandsCollected<InputFeatureType>(commands, inputOutputPairs, failedNodeIds);
    }

    /**
     * 汇总全部移除路径指令
     * <p>目前只考虑移除Way，后续如果需要移除Node或Relation了，扩展这里
     * @param ds 数据库
     * @param inputOutputPair 新旧路径对（用于执行替换）
     * @return 移除所需的指令
     */
    public List<Command> concludeRemoveCommands(
            DataSet ds, Map<InputFeatureType, Way> inputOutputPair
    ) {
        List<Command> commands = new ArrayList<>();
        for (Map.Entry<InputFeatureType, Way> inputOutputEntry : inputOutputPair.entrySet()) {
            InputFeatureType oldWay = inputOutputEntry.getKey();
            if (oldWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing an old way:\n\n")
                                + I18n.tr("Old way return value is abnormal (null), unable to get the old way.\n\n")
                                + I18n.tr("This way may not have been properly rounded or removed.")
                        // "移除某条旧路径时产生了内部错误：\n\n"
                        //         + "旧路径返回值异常（null），无法获取旧路径。"
                        //         + "该路径可能未被正确倒角或移除。"
                );
            // 至少目前（及目前计划）的功能涉及删的都是删旧线段，所有如果
            // TODO：未来如果需要删点或关系，未来再去想需要的场景、安全删除流程，最后再说重构
            if (oldWay instanceof Way) {
                Way newWay = inputOutputEntry.getValue();

                if (newWay == null)
                    throw new ColumbinaException(
                            I18n.tr("Internal error occurred while removing old way {0}:\n\n", oldWay.getUniqueId())
                                    + I18n.tr("New way return value is abnormal (null), unable to get the new way.\n\n")
                                    + I18n.tr("Old way {0} may not have been properly rounded or removed.", oldWay.getUniqueId()
                            )
                            // "移除旧路径" + oldWay.getUniqueId() + "时产生了内部错误：\n\n"
                            //         + "新路径返回值异常（null），无法获取新路径。"
                            //         + "\n\n旧路径" + oldWay.getUniqueId() + "可能未被正确倒角或移除。"
                    );
                try {
                    List<Command> cmdRmv = UtilsData.getRemoveCmd(ds, (Way) oldWay, newWay);
                    if (!cmdRmv.isEmpty()) {  // cmdRmv != null &&
                        commands.addAll(cmdRmv);
                    }
                } catch (ReplaceGeometryException | IllegalArgumentException exUtils2) {
                    UtilsUI.warnInfo(I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                            + I18n.tr("Message from Utilsplugin2:\n{0}\n\n", exUtils2.getMessage())
                            + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId()));
                    // "Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                    // + "来自Utilsplugin2的消息：\n"
                    // + exUtils2.getMessage()
                    // + "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。"
                } catch (Exception exRmv) {
                    UtilsUI.errorInfo(I18n.tr(
                            "Unexpected error occurred while removing way {0}: {1}",
                            oldWay.getUniqueId(),
                            exRmv.getMessage()
                    ));
                }
            }
        }
        // 去重防止提交重复删除
        commands = commands.stream().distinct().toList();
        return commands;
    }

    /**
     * 点击事件：绘制数据通用动作模板
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer layer; DataSet dataset; List<InputFeatureType> selections;
        boolean deleteOld; boolean selectNew; boolean copyTag;

        // 检查
        try {
            // 获取输入
            final LayerDatasetAndFeatureSelected<InputFeatureType> lyDsWs = UtilsData.getLayerDatasetAndFeaturesSelected(
                    inputFeatureType,
                    minSelection, maxSelection
            );
            if (lyDsWs == null) return;  // 用户取消操作
            layer = lyDsWs.layer;
            dataset = lyDsWs.dataset;
            selections = lyDsWs.selection;

            // 弹出参数设置窗口，获取参数
            final ParamType params = preference.getParamsAndUpdatePreference();  // 重构后preference负责弹窗，本来也就是设置preference的窗口
            if (params == null) return;  // 用户取消操作
            // 存入类成员以便concludeAddCommands之UtilsData.getAddCmd(…, params, …)
            // 和下面的ColumbinaSeqCommand(getUndoRedoInfo(…, params), …);使用
            this.params = params;
            // 获取影响整个动作模板逻辑的关键参数
            deleteOld = params.deleteOld;
            selectNew = params.selectNew;
            copyTag = params.copyTag;
        } catch (ColumbinaException | IllegalArgumentException exCheck) {
            UtilsUI.errorInfo(exCheck.getMessage());
            return;
        }

        // 绘制新路径
        AddCommandsCollected<InputFeatureType> cmdsAddAndWayPairs;
        List<Command> cmdsAdd;
        Map<InputFeatureType, Way> inputOutputPairs;  // 记录哪个输入对应哪个输出
        Map<InputFeatureType, List<Long>> failedNodeIds;  // 输入Way或Node为key，Way上处理失败的节点（或如果Node本身失败）id为value
        try {
            cmdsAddAndWayPairs = concludeAddCommands(
                    dataset, selections,
                    copyTag
            );
            cmdsAdd = cmdsAddAndWayPairs.commands;
            inputOutputPairs = cmdsAddAndWayPairs.inputOutputPairs;
            failedNodeIds = cmdsAddAndWayPairs.failedNodeIds;
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            UtilsUI.errorInfo(exAdd.getMessage());
            return;
        }

        // 绘制部分的撤销重做栈处理并正式提交执行
        if (!cmdsAdd.isEmpty()) {
            Command cmdAdd = new ColumbinaSeqCommand(getUndoRedoInfo(selections, params), cmdsAdd, "RoundCorners");
            UndoRedoHandler.getInstance().add(cmdAdd);
        }

        // 如果有拐角处理失败，则提示
        if (failedNodeIds != null && !failedNodeIds.isEmpty()) {
            boolean hasFailedNodes = false;
            String failedInfo = I18n.tr("The following corner nodes could not be rounded due to too short distance to adjacent nodes or not meeting the angle restrictions: ");
            for (Map.Entry<InputFeatureType, List<Long>> failedEntry : failedNodeIds.entrySet()) {
                if (failedEntry.getValue().isEmpty()) continue;
                failedInfo = failedInfo
                        + I18n.tr("\nWay") + failedEntry.getKey().getUniqueId()
                        + I18n.tr(": ") + failedEntry.getValue();
                hasFailedNodes = true;
            }
            if (hasFailedNodes) UtilsUI.warnInfo(failedInfo);
        }

        // 移除旧路径（目前只考虑旧路径可能需要移除）
        if (deleteOld && inputFeatureType == Way.class) {
            try {
                List<Command> cmdsRmv = concludeRemoveCommands(dataset, inputOutputPairs);
                if (!cmdsRmv.isEmpty()) {  // 如果全部都没有删除/替换，cmdsRmv为空会错错爆;
                    Command cmdRmv = new ColumbinaSeqCommand(I18n.tr("Columbina: Remove original ways"), cmdsRmv, "RemoveOldWays");
                    UndoRedoHandler.getInstance().add(cmdRmv);
                }
            } catch (ColumbinaException | IllegalArgumentException | ReplaceGeometryException exRemove) {
                UtilsUI.warnInfo(exRemove.getMessage());
                // 一个不能换不影响尝试换其他的
            }
            // TODO:选中的旧路径之间有交点且不与其他路径连接时，因为现在删/换数条线是打包到一次undoRedo中同时操作。
            //  删way1时认为交点在way2上，way2认为在way1上，一起做这些交点不会被删除/替换，需要手动处理。
        }

        // 选中新路径
        if (selectNew) {
            List<Way> newWays = new ArrayList<>();
            for (Map.Entry<InputFeatureType, Way> inputOutputEntry: inputOutputPairs.entrySet()) newWays.add(inputOutputEntry.getValue());
            dataset.setSelected(newWays);
        }
    }
}


