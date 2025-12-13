package yakxin.columbina.abstractClasses.actionMiddle;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryCommand;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
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
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import java.util.*;

/**
 * 圆角、斜角、缓和曲线用的模板类
 * <p>这几个功能有如下共同点：
 * <ul>
 *     <li>都是输入Way</li>
 *     <li>支持批量操作，即每组输入只有一条路径，对每条路径分别操作</li>
 *     <li>一条路径上有多个需要处理的拐角节点，可能有部分失败的情况</li>
 *     <li>需要替换原有路径</li>
 * </ul>
 * @param <GeneratorType> 生成器泛型
 * @param <PreferenceType> 首选项泛型
 * @param <ParamType> 输入参数泛型
 */
public abstract class ActionWithBatchWays<
        GeneratorType extends AbstractGenerator<ParamType>,  // 生成器泛型
        PreferenceType extends AbstractPreference<ParamType>,  // 首选项泛型
        ParamType extends AbstractParams> // 输入参数泛型
        extends AbstractDrawingAction<GeneratorType, PreferenceType, ParamType>
{
    private final int minSelection;
    private final int maxSelection;
    private Map<Way, Way> inputOutputPairs = new HashMap<>();  // 记录哪个输入对应哪个输出


    /// 构造函数和具体重写方法
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
    public ActionWithBatchWays(
            String name, String iconName, String description, Shortcut shortcut,
            GeneratorType generator, PreferenceType preference,
            int minSelection, int maxSelection
    ) {
        super(name, iconName, description, shortcut,
                generator, preference
        );
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
    }

    @Override
    public int checkInputNum(ColumbinaInput totalInput) {
        boolean ifOnlyContain2NodeWays = true;
        // 没有选定路径、（有限制时）选择太少或太多中止流程
        if (totalInput.isEmpty(Way.class))
            throw new IllegalArgumentException(I18n.tr("No way is selected."));
        // 检查是否只包含2点路径
        for (Way way : totalInput.getWays()) {
            if ((!way.isClosed() && way.getNodes().size() >= 3) || (way.isClosed() && way.getNodes().size() >= 4)) {
                ifOnlyContain2NodeWays = false;
                break;
            }
        }
        if (ifOnlyContain2NodeWays) throw new IllegalArgumentException("All selected ways only contain 2 nodes, and cannot find corners.");

        // 参数检查
        if (minSelection != NO_LIMITATION_ON_INPUT_NUM && totalInput.getInputNum(Way.class) < minSelection)
            throw new IllegalArgumentException(I18n.tr("Too few ways are selected, should be grater than {0}.", minSelection));
        if (maxSelection != NO_LIMITATION_ON_INPUT_NUM && totalInput.getInputNum(Way.class) > maxSelection)
            throw new IllegalArgumentException(I18n.tr("Too many ways are selected, should be less than {0}.", maxSelection));
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

    @Override
    public int checkInputDetails(List<ColumbinaSingleInput> singleInputs) {return CHECK_OK;}  // 这个模板下暂时没有需要具体检查的

    @Override
    public List<ColumbinaSingleInput> splitBatchInputs(ColumbinaInput inputs) {
        // 对于这一类，单组输入都是一条路径
        List<ColumbinaSingleInput> singleInputs = new ArrayList<>();
        for (Way wayToBeProcessed : inputs.getWays()) {
            singleInputs.add(new ColumbinaSingleInput(
                    new ArrayList<Node>(),
                    new ArrayList<Way>(Collections.singleton(wayToBeProcessed))
            ));
        }
        return singleInputs;
    }

    @Override
    public List<Command> concludeAddCommands(
            DataSet ds, List<ColumbinaSingleInput> singleInputs,
            boolean copyTag
    ) {
        List<Command> commands = new ArrayList<>();
        inputOutputPairs = new HashMap<>();  // 输入节点/路径k与新绘制的路径v的打包对
        Map<Way, List<Long>> failedNodeIds = new HashMap<>();  // 处理输入节点/路径k与处理时k上失败的节点v的打包对
        // 处理路径
        for (ColumbinaSingleInput singleInput : singleInputs) {  // 分别处理每个输入路径
            try {  // 一条路径出错尽可能不影响其他的
                NewNodeWayCommands newNWCmd = getAddCmd(ds, singleInput, generator, params, copyTag);  // 获取路径

                if (newNWCmd != null) {  // 应该不会和已提交但未执行（进入ds）的重复提交
                    commands.addAll(newNWCmd.addCommands);
                    inputOutputPairs.put(singleInput.ways.getFirst(), newNWCmd.newWay);
                    failedNodeIds.put(singleInput.ways.getFirst(), newNWCmd.failedNodeIds);
                }
                else UtilsUI.warnInfo(I18n.tr(
                        "Algorithm did not return at least 2 nodes to form a way for way {0}, this way was not processed.",
                        singleInput.ways.getFirst().getUniqueId()
                ));
            } catch (Exception exAdd) {
                UtilsUI.errorInfo(I18n.tr("Unexpected error occurred while processing way {0}: {1}",
                        singleInput.ways.getFirst().getUniqueId(), exAdd.getMessage()
                ));
            }
        }

        if (commands.isEmpty()) {  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));
        }
        // 去重防止提交重复添加
        commands = commands.stream().distinct().toList();

        // 提示失败信息
        showFailedProcessedCorner(failedNodeIds);

        return commands;
    }

    // 对于这个模板来说，需要移除原有路径
    @Override
    public List<Command> concludeRemoveCommands(
            DataSet ds
    ) {
        // TODO:选中的旧路径之间有交点且不与其他路径连接时，因为现在删/换数条线是打包到一次undoRedo中同时操作。
        //  删way1时认为交点在way2上，way2认为在way1上，一起做这些交点不会被删除/替换，需要手动处理。
        List<Command> commands = new ArrayList<>();
        for (Map.Entry<Way, Way> inputOutputEntry : inputOutputPairs.entrySet()) {
            Way oldWay = inputOutputEntry.getKey();
            if (oldWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing an old way:\n\n")
                                + I18n.tr("Old way return value is abnormal (null), unable to get the old way.\n\n")
                                + I18n.tr("This way may not have been properly rounded or removed.")
                        // "移除某条旧路径时产生了内部错误：\n\n"
                        //         + "旧路径返回值异常（null），无法获取旧路径。"
                        //         + "该路径可能未被正确倒角或移除。"
                );
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
                List<Command> cmdRmv = getRemoveCmd(ds, oldWay, newWay);
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
        // 去重防止提交重复删除
        commands = commands.stream().distinct().toList();
        return commands;
    }

    @Override
    public List<OsmPrimitive> getWhatToSelectAfterDraw() {
        List<OsmPrimitive> newWays = new ArrayList<>();
        for (Map.Entry<Way, Way> inputOutputEntry: inputOutputPairs.entrySet()) newWays.add(inputOutputEntry.getValue());
        return newWays;
    }

    @Override
    public Map<String, String> getNewWayTags(ColumbinaSingleInput singleInput) {
        Map<String, String> newWayTags = new HashMap<>();
        if (singleInput.ways != null && singleInput.ways.size() == 1)
            newWayTags = singleInput.ways.getFirst().getInterestingTags();  // 读取原Way的tag
        return newWayTags;
    }

    /// 特有方法
    /**
     * 调用Generator获得绘制单条新路径所需指令
     * <p>将会调用具体生成器的getNewNodeWayForSingleInput方法，由于输入要素为ColumbinaSingleInput类型，具体生成器类需要自行判断、转换为需要的类型
     * @param ds 数据集
     * @param singleInput 选定输入要素
     * @param generator 生成器
     * @param params 输入参数
     * @param copyTag 是否复制标签
     * @return 对于一组输入产生的、绘制单条路径所需的指令
     */
    private NewNodeWayCommands getAddCmd (
            DataSet ds, ColumbinaSingleInput singleInput,
            GeneratorType generator, ParamType params,
            boolean copyTag) {
        // 调用生成传入的函数计算路径
        ColumbinaSingleOutput singleOutput = generator.getOutputForSingleInput(singleInput, params);
        if (singleOutput == null) return null;
        if (!singleOutput.ifCanMakeAWay()) return null;
        List<Node> newNodes = singleOutput.newNodes;
        List<Long> failedNodeIds = singleOutput.failedNodes;
        List<Command> addCommands = new LinkedList<>();

        // 画新线
        Way newWay = singleOutput.linkNodesToWay();

        // 复制原Way标签
        if (copyTag) {
            Map<String, String> keys = getNewWayTags(singleInput);
            if (newWay != null && keys != null)
                newWay.setKeys(keys);
        }

        // 正式构建绘制命令
        if (newWay != null) {
            for (Node n : newNodes.stream().distinct().toList()) {  // 路径内部可能有节点复用（如闭合线），去重
                if (!ds.containsNode(n))  // 新路径的节点在ds中未绘制（不是复用的）才准备绘制
                    addCommands.add(new AddCommand(ds, n));  // 添加节点到命令序列
            }
            addCommands.add(new AddCommand(ds, newWay));  // 添加线到命令序列
        }

        return new NewNodeWayCommands(newWay, addCommands, failedNodeIds);
    }

    /**
     * 移除/替换一条旧路径及无用节点的指令，删多条把这个函数放在for里面一个个删
     * <p>移除/替换操作是ActionWithBatchWays里面3个功能统一的，所以统一这里实现
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
                // "Columbina尝试调用Utilsplugin2插件之「替换几何图形」功能替换旧路径，但失败。\n\n"
                // "用户在Utilsplugin2的窗口中取消了替换操作。\n"
                // "\n\n旧路径" + oldWay.getUniqueId() + "未被移除。"
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
                // 不能用ds.ContainsNode(n)判断删没删，因为cmdDel还没提交，delCommands内部重复删除会炸
                for (Node n : oldWay.getNodes().stream().distinct().toList()) {
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
     * 弹出消息提示处理失败的节点
     * @param failedNodeIds 存在失败情况的路径和具体失败的节点列表组成的打包对
     */
    public void showFailedProcessedCorner(Map<Way, List<Long>> failedNodeIds) {
        // 如果有拐角处理失败，则提示
        if (failedNodeIds != null && !failedNodeIds.isEmpty()) {
            boolean hasFailedNodes = false;
            String failedInfo = I18n.tr("The following corner nodes could not be rounded due to too short distance to adjacent nodes or not meeting the angle restrictions: ");
            for (Map.Entry<Way, List<Long>> failedEntry : failedNodeIds.entrySet()) {
                if (failedEntry.getValue().isEmpty()) continue;
                failedInfo = failedInfo
                        + I18n.tr("\nWay") + failedEntry.getKey().getUniqueId()
                        + I18n.tr(": ") + failedEntry.getValue();
                hasFailedNodes = true;
            }
            if (hasFailedNodes) UtilsUI.warnInfo(failedInfo);
        }
    }

    /// 内部类
    private static final class NewNodeWayCommands {
        public final Way newWay;
        public final List<Command> addCommands;
        public final List<Long> failedNodeIds;

        public NewNodeWayCommands(Way newWay, List<Command> addCommands, List<Long> failedNodeIds) {
            this.newWay = newWay;
            this.addCommands = addCommands;
            this.failedNodeIds = failedNodeIds;
        }
    }
}


