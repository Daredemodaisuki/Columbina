package yakxin.columbina.abstractClasses;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryCommand;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.ColumbinaSeqCommand;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.data.dto.outputs.ColumbinaOutputIntent;
import yakxin.columbina.data.dto.outputs.ColumbinaSingleOutput;
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 总的功能操作逻辑：根据输入的节点或者路径绘制新路径
 * <p>不管具体的参数、生成方式是什么了，只管通用逻辑，一切差异化的东西在具体的action类实现，具体的参数、生成方式分别在各自类实现之后传实体进来。
 * <p>通用的逻辑是：获取当前选择输入（目前支持Way或Node）；弹出参数设置窗口，获取参数；对每个输入Way或Node使用生成器绘制新路径，并记录输入输出对；
 * 对输入中处理失败的节点提示；移除旧路径（目前只考虑输入Way可能需要移除，节点有需求再扩充）
 *
 * <p>〔自1.0.4起〕整合了原先的中间层 ActionWithBatchWays 和 ActionWithNodeWay，
 * 非批量输入等同于批量输入一组，统一在此类中处理。
 *
 * @param <GeneratorType> 具体的生成器类
 * @param <PreferenceType> 具体的首选项类
 * @param <ParamType> 具体的参数类
 */
public abstract class AbstractDrawingAction <
        GeneratorType extends AbstractGenerator<ParamType>,  // 生成器泛型
        PreferenceType extends AbstractPreference<ParamType>,  // 首选项泛型
        ParamType extends AbstractParams>  // 输入参数泛型
        extends JosmAction {
    public static final int NO_LIMITATION_ON_INPUT_NUM = -1;
    public static final int NO_THIS_KIND_OF_INPUT = 0;
    public static final int CHECK_OK = 0;
    public static final int CHECK_OK_BUT_WARN = 1;
    public static final int USER_CANCEL = 2;

    /// 具体的参数、生成方式
    protected final GeneratorType generator;
    protected final PreferenceType preference;  // preference是final但是不影响它内部自己变
    protected ParamType params;  // params将在执行点击事件时具体获取，每次获取可能不一致
    public final String iconName;

    /// 输入输出追踪（由 concludeAddCommands 负责填充）
    private Map<Way, Way> inputOutputWayPairs = new HashMap<>();  // 输入旧路径 → 输出新路径（用于移除旧路径和选中新路径）
    private List<OsmPrimitive> outputRepresentatives = new ArrayList<>();  // 输出代表性要素（用于选中新路径）
    private Map<ColumbinaSingleInput, ColumbinaSingleOutput> lastIoPairs = new HashMap<>();  // 输入输出对（用于失败信息提示）

    /// 所有需要由具体action类定义的东西
    /**
     * 获取撤销重做栈的说明
     * @param inputs 输入要素
     * @param params 输入参数
     * @return 说明文本（I18n后）
     */
    public abstract String getUndoRedoInfo(ColumbinaInput inputs, ParamType params);

    /**
     * 拆分批量输入为单组输入
     * <p>对于支持批量操作的功能（如倒圆角、倒斜角等），将每个Way拆为一组；
     * <p>对于不支持批量操作的功能（如定向画线、曲线连接等），将所有输入包装为一组。
     * @param totalInput 输入要素
     * @return 单组输入列表
     */
    public abstract List<ColumbinaSingleInput> splitBatchInputs(ColumbinaInput totalInput);

    /**
     * 检查输入要素的数量是否合法
     * <p>这里不检查具体的输入要求（比如节点是否在路径上）。
     * <p>具体的数量要求应在具体操作类中定义，UtilsData中有同名方法，具体实现时可以调用，如果数值不合法将抛出IllegalArgumentException；
     * <p>同名方法不会返回任何值，说明检查通过，在具体类的实现中，同名方法检查OK后具体类就可以返回CHECK_OK（0）；
     * <p>如果检查通过后有一些询问（比如选择太多了是否一次处理），用户取消返回USER_CANCEL（2）。
     * @see UtilsData#checkInputNum(ColumbinaInput, int, int, int, int)
     * @param totalInput 全部输入要素
     * @return 检查结果状态
     */
    public abstract int checkInputNum(ColumbinaInput totalInput);

    /**
     * 检查输入要素内部具体的内容是否符合要求
     * <p>检查OK后具体类可以返回CHECK_OK（0）；
     * <p>如果需要询问，用户取消返回USER_CANCEL（2）；
     * <p>如果不符合要求，抛出ColumbinaException。
     * <p>在检查期间，部分操作的具体action类可能会预计算一些东西，预计算的东西可以直接赋值给ColumbinaSingleInput的快捷传递中间量（quickPrecomputedData），后续给到生成器可以减少重复计算。
     * @param singleInputs 单组输入要素构成的列表
     * @return 检查结果状态
     */
    public abstract int checkInputDetails(List<ColumbinaSingleInput> singleInputs);

    /**
     * 获取新绘制路径所需的标签，为 concludeAddCommands 所用
     * <p>默认返回空映射，如需复制输入路径标签可在具体类中重写。
     * @param singleInput 单组输入要素
     * @return 标签映射
     */
    public Map<String, String> getNewWayTags(ColumbinaSingleInput singleInput) {
        return new HashMap<>();
    }

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
        this.iconName = iconName;
        // params将在执行点击事件时具体获取，每次获取可能不一致
    }

    /**
     * 点击事件：绘制数据通用动作模板
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet dataSet; ColumbinaInput totalInput; List<ColumbinaSingleInput> singleInputs;
        boolean deleteOld; boolean selectNew; boolean copyTag;

        /// 检查
        try {
            totalInput = new ColumbinaInput();
            dataSet = totalInput.getDataSet();

            // 数量检查（不可接受的数量将在checkInputNum抛IllegalArgumentException）
            if (checkInputNum(totalInput) == USER_CANCEL) return;  // 用户取消时直接退出
            // 批量输入拆分为单组输入
            singleInputs = splitBatchInputs(totalInput);
            // 具体输入检查（有问题将在checkInputDetails抛ColumbinaException）
            if (checkInputDetails(singleInputs) == USER_CANCEL) return;

            // 弹出参数设置窗口，获取参数
            // 重构后preference负责弹窗，本来也就是设置preference的窗口
            // 把input也给到getParamsAndUpdatePreference，以便窗口可以实时计算一些内容并反馈给用户
            final ParamType params = preference.getParamsAndUpdatePreference(totalInput);
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

        /// 绘制新路径
        List<Command> cmdsAdd;
        try {
            cmdsAdd = concludeAddCommands(dataSet, singleInputs, copyTag);
            if (cmdsAdd == null || cmdsAdd.isEmpty())
                throw new ColumbinaException(I18n.tr("No input was successfully processed."));
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            UtilsUI.errorInfo(exAdd.getMessage());
            return;
        }

        // 绘制部分的撤销重做栈处理并正式提交执行
        Command cmdAdd = new ColumbinaSeqCommand(getUndoRedoInfo(totalInput, params), cmdsAdd, iconName);
        UndoRedoHandler.getInstance().add(cmdAdd);

        /// 移除旧路径（如果需要的话）
        if (deleteOld) {
            try {
                List<Command> cmdsRmv = concludeRemoveCommands(dataSet);
                if (cmdsRmv == null || !cmdsRmv.isEmpty()) {  // 如果全部都没有删除/替换，cmdsRmv为空会错错爆;
                    Command cmdRmv = new ColumbinaSeqCommand(I18n.tr("Columbina: Remove original ways"), cmdsRmv, "RemoveOldWays");
                    UndoRedoHandler.getInstance().add(cmdRmv);
                }
            } catch (ColumbinaException | IllegalArgumentException | ReplaceGeometryException exRemove) {
                UtilsUI.warnInfo(exRemove.getMessage());
                // 一个不能换不影响尝试换其他的
            }
        }

        /// 选中新路径
        if (selectNew) {
            List<OsmPrimitive> whatToSelectAfterDraw = getWhatToSelectAfterDraw();
            if (whatToSelectAfterDraw != null && !whatToSelectAfterDraw.isEmpty()) dataSet.setSelected(whatToSelectAfterDraw);
        }
    }

    /// 默认实现的模板方法

    /**
     * 将输入要素传入generator获取并汇总全部添加指令
     * <p>统一使用批量循环逻辑：遍历每个单组输入，调用生成器，汇总意图和命令。
     * <p>非批量操作等同于批量操作一组。
     * <p>如果一组输入中有部分失败，弹窗警告，不影响全流程；
     * <p>如果一组输入完全失败，在循环中跳过以不影响其他组输入。
     * <p>如果不需要添加，返回空的列表。
     * <p>如果全部都处理失败而没有添加指令，抛出ColumbinaException。
     * @param ds 数据集
     * @param singleInputs 选定的单组输入列表
     * @param copyTag 是否拷贝标签
     * @return 对于全部输入产生的添加指令列表
     */
    public List<Command> concludeAddCommands(
            DataSet ds, List<ColumbinaSingleInput> singleInputs,
            boolean copyTag
    ) {
        List<ColumbinaOutputIntent<?>> intents = new ArrayList<>();
        inputOutputWayPairs = new HashMap<>();
        outputRepresentatives = new ArrayList<>();
        lastIoPairs = new HashMap<>();

        for (ColumbinaSingleInput singleInput : singleInputs) {
            // 调用生成器计算
            ColumbinaSingleOutput singleOutput = generator.getOutputForSingleInput(singleInput, params);
            if (singleOutput == null) continue;
            if (!singleOutput.isValid()) continue;

            // 记录输入输出对
            lastIoPairs.put(singleInput, singleOutput);

            // 收集新线（目前假定singleOutput只输出一条新线）
            Way newWay = (Way) singleOutput.representatives.get(0);

            // 复制原Way标签
            if (copyTag) {
                Map<String, String> wayTags = getNewWayTags(singleInput);
                newWay.setKeys(wayTags);
            }

            intents.addAll(singleOutput.outputIntents);
            outputRepresentatives.add(newWay);

            // 如果单组输入中有Way，记录输入输出Way对（用于后续移除旧路径）
            if (singleInput.ways != null && !singleInput.ways.isEmpty()) {
                // 对于批量路径输入（每组1条Way），记录第一条Way
                // 对于非批量输入，如果包含Way也可以记录（但通常非批量不需要移除）
                inputOutputWayPairs.put(singleInput.ways.get(0), newWay);
            }
        }

        // 转为指令（toCommands已去重）
        List<Command> commands = ColumbinaOutputIntent.toCommands(intents, ds);

        if (commands.isEmpty())  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));

        // 提示失败信息
        // TODO：汇总生成器和解释器的错误信息
        showFailureInfo(lastIoPairs);

        return commands;
    }

    /**
     * 汇总全部移除指令
     * <p>对 inputOutputWayPairs 中记录的每对旧路径→新路径，尝试替换或删除旧路径。
     * <p>对于已上传的路径，使用UtilsPlugin2的 ReplaceGeometryCommand 替换旧路径；
     * <p>对于本地新绘制的、未上传的路径，检查后直接删除；
     * <p>如果 inputOutputWayPairs 为空（不需要移除），返回空的命令列表。
     * @param ds 数据集
     * @return 对于全部输入产生的移除指令列表
     */
    public List<Command> concludeRemoveCommands(DataSet ds) {
        if (inputOutputWayPairs.isEmpty()) return new ArrayList<>();

        // TODO:选中的旧路径之间有交点且不与其他路径连接时，因为现在删/换数条线是打包到一次undoRedo中同时操作。
        //  删way1时认为交点在way2上，way2认为在way1上，一起做这些交点不会被删除/替换，需要手动处理。
        List<Command> commands = new ArrayList<>();
        for (Map.Entry<Way, Way> inputOutputEntry : inputOutputWayPairs.entrySet()) {
            Way oldWay = inputOutputEntry.getKey();
            if (oldWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing an old way:\n\n")
                                + I18n.tr("Old way return value is abnormal (null), unable to get the old way.\n\n")
                                + I18n.tr("This way may not have been properly rounded or removed.")
                );
            Way newWay = inputOutputEntry.getValue();

            if (newWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing old way {0}:\n\n", oldWay.getUniqueId())
                                + I18n.tr("New way return value is abnormal (null), unable to get the new way.\n\n")
                                + I18n.tr("Old way {0} may not have been properly rounded or removed.", oldWay.getUniqueId()
                        )
                );
            try {
                List<Command> cmdRmv = getRemoveCmd(ds, oldWay, newWay);
                if (!cmdRmv.isEmpty()) {
                    commands.addAll(cmdRmv);
                }
            } catch (ReplaceGeometryException | IllegalArgumentException exUtils2) {
                UtilsUI.warnInfo(I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                        + I18n.tr("Message from Utilsplugin2:\n{0}\n\n", exUtils2.getMessage())
                        + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId()));
            } catch (Exception exRmv) {
                UtilsUI.errorInfo(I18n.tr(
                        "Unexpected error occurred while removing way {0}: {1}",
                        oldWay.getUniqueId(),
                        exRmv.getMessage()
                ));
            }
        }
        // 去重防止提交重复删除
        commands = commands.stream().distinct().collect(Collectors.toList());
        return commands;
    }

    /**
     * 获取需要选中的新路径
     * <p>根据 concludeAddCommands 中记录的输出代表性要素返回绘图后需要选择的对象。
     * @return 需要选择的对象列表
     */
    public List<OsmPrimitive> getWhatToSelectAfterDraw() {
        return outputRepresentatives;
    }

    /// 工具方法

    /**
     * 移除/替换一条旧路径及无用节点的指令
     * @param ds 当前数据库
     * @param oldWay 希望移除的旧路径
     * @param newWay 用于替换的新路径
     * @return 指令列表
     */
    private List<Command> getRemoveCmd(DataSet ds, Way oldWay, Way newWay) {
        List<Command> removeCommands = new ArrayList<>();

        // 既有路径替换
        if (oldWay.getId() != 0) {
            ReplaceGeometryCommand seqCmdRep = ReplaceGeometryUtils.buildReplaceWithNewCommand(oldWay, newWay);
            if (seqCmdRep == null) {
                UtilsUI.warnInfo(
                        I18n.tr("Columbina attempted to use Utilsplugin2''s ''Replace Geometry'' function to replace the old way, but failed.\n\n")
                                + I18n.tr("The user canceled the replacement operation in the Utilsplugin2 window.\n\n")
                                + I18n.tr("Old way {0} was not removed.", oldWay.getUniqueId())
                );
            }
            else {
                List<Command> cmdRep = UtilsData.tryGetCommandsFromSeqCmd(seqCmdRep);
                removeCommands.addAll(cmdRep);
            }
        }
        // 新路径删除
        else {
            if (oldWay.getReferrers().isEmpty()) {  // 旧路径有关系，连同节点都不删
                removeCommands.add(new DeleteCommand(ds, oldWay));  // 去除路径
                // 去除节点（闭合曲线会删闭合点2次，自交路径也会导致重复删除，需要去重）
                for (Node n : oldWay.getNodes().stream().distinct().collect(Collectors.toList())) {
                    boolean canBeDeleted = !n.isTagged();  // 有tag不删
                    for (OsmPrimitive ref : n.getReferrers()) {
                        if (!(ref instanceof Way && ref.equals(oldWay))) {
                            canBeDeleted = false;  // 被其他路径或关系使用，不删
                            break;
                        }
                    }
                    if (canBeDeleted) removeCommands.add(new DeleteCommand(ds, n));
                }
            } else {
                UtilsUI.warnInfo(I18n.tr(
                        "Old way {0} is still referenced by relations, not removed.",
                        oldWay.getUniqueId()
                ));
            }
        }
        return removeCommands;
    }

    /**
     * 如果有失败，则弹出消息提示处理失败的信息
     * @param ioPairs 单组输入和单组输出的打包对
     */
    private void showFailureInfo(Map<ColumbinaSingleInput, ColumbinaSingleOutput> ioPairs) {
        // TODO：需要继续整理格式
        StringBuilder partiallyFailed = new StringBuilder(I18n.tr("Partial failures occurring when processing following inputs:\n"));
        StringBuilder failed = new StringBuilder(I18n.tr("Failures occurring when processing following inputs:\n"));
        boolean hasPartiallyFailedInput = false;
        boolean hasFailedInput = false;
        for (Map.Entry<ColumbinaSingleInput, ColumbinaSingleOutput> ioEntry : ioPairs.entrySet()) {
            if (ioEntry != null && ioEntry.getKey() != null && ioEntry.getValue() != null) {
                ColumbinaSingleInput input = ioEntry.getKey();
                ColumbinaSingleOutput output = ioEntry.getValue();
                switch (output.status) {
                    case FAILED:
                        failed.append("-").append(UtilsData.featureListToString(input.getMixedInputList())).append(":")
                                .append(output.generalInfo).append("\n");
                        hasFailedInput = true;
                        break;
                    case PARTIALLY_FAILED:
                        partiallyFailed.append("-").append(UtilsData.featureListToString(input.getMixedInputList())).append(":\n")
                                .append(" - >").append(output.concludeFailedInfo()).append("\n");
                        hasPartiallyFailedInput = true;
                        break;
                }
            }
        }
        String warnInfo = "";
        if (hasFailedInput) warnInfo += failed + "\n";
        if (hasPartiallyFailedInput) warnInfo += partiallyFailed + "\n";
        if (hasFailedInput || hasPartiallyFailedInput) UtilsUI.warnInfo(warnInfo);
    }

    /// 批量路径输入的默认实现辅助方法

    /**
     * 批量路径输入的默认数量检查（用于原先继承 ActionWithBatchWays 的功能）
     * <p>检查Way数量是否在 [minSelection, maxSelection] 范围内，超过5条路径时弹出确认对话框。
     * @param totalInput 全部输入要素
     * @param minSelection 最小Way选择数
     * @param maxSelection 最大Way选择数
     * @return 检查结果状态
     */
    protected int defaultBatchWaysCheckInputNum(ColumbinaInput totalInput, int minSelection, int maxSelection) {
        UtilsData.checkInputNum(totalInput, NO_LIMITATION_ON_INPUT_NUM, NO_LIMITATION_ON_INPUT_NUM, minSelection, maxSelection);
        if (totalInput.getInputNum(Way.class) > 5) {
            int confirmTooMany = JOptionPane.showConfirmDialog(
                    null,
                    I18n.tr("Are you sure you want to process {0} ways at once? This may take a long time.", totalInput.getInputNum(Way.class)),
                    I18n.tr("Columbina"),
                    JOptionPane.YES_NO_OPTION
            );
            if (confirmTooMany == JOptionPane.NO_OPTION) return USER_CANCEL;
        }
        return CHECK_OK;
    }

    /**
     * 批量路径输入的默认具体检查（用于原先继承 ActionWithBatchWays 的功能）
     * <p>检查所有Way是否不只是2点路径。
     * @param singleInputs 单组输入要素构成的列表
     * @return 检查结果状态
     */
    protected int defaultBatchWaysCheckInputDetails(List<ColumbinaSingleInput> singleInputs) {
        boolean ifOnlyContain2NodeWays = true;
        for (ColumbinaSingleInput singleInput : singleInputs) {
            Way way = singleInput.ways.get(0);
            if ((!way.isClosed() && way.getNodes().size() >= 3) || (way.isClosed() && way.getNodes().size() >= 4)) {
                ifOnlyContain2NodeWays = false;
                break;  // 有正常路径就行
            }
        }
        if (ifOnlyContain2NodeWays)
            throw new ColumbinaException("All selected ways only contain 2 nodes, and cannot find corners.");

        return CHECK_OK;
    }

    /**
     * 批量路径输入的默认拆分（用于原先继承 ActionWithBatchWays 的功能）
     * <p>将每个Way拆为一组单组输入。
     * @param totalInput 全部输入要素
     * @return 单组输入列表
     */
    protected List<ColumbinaSingleInput> defaultBatchWaysSplitInputs(ColumbinaInput totalInput) {
        List<ColumbinaSingleInput> singleInputs = new ArrayList<>();
        for (Way wayToBeProcessed : totalInput.getWays()) {
            singleInputs.add(new ColumbinaSingleInput(
                    new ArrayList<>(),
                    new ArrayList<>(Collections.singleton(wayToBeProcessed))
            ));
        }
        return singleInputs;
    }

    /**
     * 批量路径输入的默认标签复制（用于原先继承 ActionWithBatchWays 的功能）
     * <p>从输入Way复制标签到新Way。
     * @param singleInput 单组输入要素
     * @return 标签映射
     */
    protected Map<String, String> defaultBatchWaysGetNewWayTags(ColumbinaSingleInput singleInput) {
        Map<String, String> newWayTags = new HashMap<>();
        if (singleInput.ways != null && singleInput.ways.size() == 1)
            newWayTags = singleInput.ways.get(0).getInterestingTags();  // 读取原Way的tag
        return newWayTags;
    }

    /**
     * 非批量输入的默认拆分（用于原先继承 ActionWithNodeWay 的功能）
     * <p>将所有输入包装为一组。
     * @param totalInput 全部输入要素
     * @return 单组输入列表（只有一个元素）
     */
    protected List<ColumbinaSingleInput> defaultNonBatchSplitInputs(ColumbinaInput totalInput) {
        return new ArrayList<>(Collections.singleton(new ColumbinaSingleInput(totalInput)));
    }
}
