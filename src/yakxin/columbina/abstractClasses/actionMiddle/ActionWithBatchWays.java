package yakxin.columbina.abstractClasses.actionMiddle;

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
import yakxin.columbina.data.dto.outputs.ColumbinaOutputIntent;
import yakxin.columbina.data.dto.outputs.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

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
        // 参数数量检查，不检查是否只包含2点路径（由checkInputDetails判断）
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

    @Override
    public int checkInputDetails(List<ColumbinaSingleInput> singleInputs) {
        // 检查是否只包含2点路径
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

    @Override
    public List<ColumbinaSingleInput> splitBatchInputs(ColumbinaInput totalInput) {
        // 对于这一类，单组输入都是一条路径
        List<ColumbinaSingleInput> singleInputs = new ArrayList<>();
        for (Way wayToBeProcessed : totalInput.getWays()) {
            singleInputs.add(new ColumbinaSingleInput(
                    new ArrayList<>(),
                    new ArrayList<>(Collections.singleton(wayToBeProcessed))
            ));
        }
        return singleInputs;
    }

    @Override
    public List<Command> concludeAddCommands(
            DataSet ds, List<ColumbinaSingleInput> singleInputs,
            boolean copyTag
    ) {
        List<ColumbinaOutputIntent<?>> intents = new ArrayList<>();
        inputOutputPairs = new HashMap<>();  // 输入节点/路径k与新绘制的路径v的打包对
        Map<ColumbinaSingleInput, ColumbinaSingleOutput> ioPairs = new HashMap<>();  // 处理输入节点/路径k与处理时k上失败的节点v的打包对
        // 处理路径
        for (ColumbinaSingleInput singleInput : singleInputs) {  // 分别处理每个输入路径
            // 调用生成传入的函数计算路径
            ColumbinaSingleOutput singleOutput = generator.getOutputForSingleInput(singleInput, params);
            if (singleOutput == null) continue;
            if (!singleOutput.isValid()) continue;
            
            // 记录输入输出对
            ioPairs.put(singleInput, singleOutput);
            
            // 收集新线（目前假定singleOutput只输出一条新线）
            Way newWay = (Way) singleOutput.representatives.get(0);
            
            // 复制原Way标签
            if (copyTag) {
                Map<String, String> wayTags = getNewWayTags(singleInput);
                newWay.setKeys(wayTags);
            }
            
            intents.addAll(singleOutput.outputIntents);
            inputOutputPairs.put(singleInput.ways.get(0), newWay);
        }
        // 转为指令（toCommands已去重）
        List<Command> commands = ColumbinaOutputIntent.toCommands(intents, ds);

        if (commands.isEmpty())  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));

        // 提示失败信息
        // TODO：汇总生成器和解释器的错误信息
        showFailureInfo(ioPairs);

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
        commands = commands.stream().distinct().collect(Collectors.toList());
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
            newWayTags = singleInput.ways.get(0).getInterestingTags();  // 读取原Way的tag
        return newWayTags;
    }

    /// 特有方法
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
    public void showFailureInfo(Map<ColumbinaSingleInput, ColumbinaSingleOutput> ioPairs) {
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
}


