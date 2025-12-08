package yakxin.columbina.abstractClasses;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.ColumbinaSeqCommand;
import yakxin.columbina.data.dto.AddCommandsCollected;
import yakxin.columbina.data.dto.LayerDatasetAndWaySelected;
import yakxin.columbina.data.dto.NewNodeWayCommands;
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsUI;

import java.awt.event.ActionEvent;
import java.util.*;

/**
 * 总的功能操作逻辑
 * <p>不管具体的参数、生成方式是什么了，只管通用逻辑，一切差异化的东西在具体的action类实现，具体的参数、生成方式分别在各自类实现之后传实体进来。
 * @param <GeneratorType> 具体的生成器类
 * @param <PreferenceType> 具体的首选项类
 * @param <ParamType> 具体的参数类
 */
public abstract class AbstractDrawingAction
        <GeneratorType extends AbstractGenerator<ParamType>, PreferenceType extends AbstractPreference<ParamType>, ParamType extends AbstractParams>
        extends JosmAction
{
    /// 具体的参数、生成方式
    private final GeneratorType generator;
    private final PreferenceType preference;  // preference是final但是不影响它内部自己变
    private ParamType params;  // params将在执行点击事件时具体获取，每次获取可能不一致

    /// 所有需要由具体action类定义的东西
    public abstract String getUndoRedoInfo(List<Way> selectedWays, ParamType params);
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
    }

    /**
     * 通过传入的generator汇总全部添加指令
     * @param ds 数据库
     * @param selectedWays 选定的旧路径
     * @param copyTag 是否拷贝标签
     * @return 添加所需的指令、新旧路径对、失败的节点的打包
     */
    public AddCommandsCollected concludeAddCommands(
            DataSet ds, List<Way> selectedWays,
            boolean copyTag
    ) {
        List<Command> commands = new ArrayList<>();
        Map<Way, Way> oldNewWayPairs = new HashMap<>();
        Map<Way, List<Long>> failedNodeIds = new HashMap<>();

        // 处理路径
        for (Way w : selectedWays) {  // 分别处理每条路径
            try {  // 一条路径出错尽可能不影响其他的
                NewNodeWayCommands newNWCmd = UtilsData.getAddCmd(ds, w, generator, params, copyTag);  // 获取路径

                if (newNWCmd != null) {  // 应该不会和已提交但未执行（进入ds）的重复提交
                    commands.addAll(newNWCmd.addCommands);
                    oldNewWayPairs.put(w, newNWCmd.newWay);
                    failedNodeIds.put(w, newNWCmd.failedNodeIds);
                }
                else UtilsUI.warnInfo(I18n.tr(
                        "Algorithm did not return at least 2 nodes to form a way for way {0}, this way was not processed.",
                        w.getUniqueId()
                ));
            } catch (Exception exAdd) {
                UtilsUI.errorInfo(I18n.tr("Unexpected error occurred while processing way {0}: {1}",
                        w.getUniqueId(), exAdd.getMessage()
                ));
            }
        }

        if (commands.isEmpty()) {  // 未能成功生成一条线
            throw new ColumbinaException(I18n.tr("Failed to generate any new way."));
        }
        // 去重防止提交重复添加
        commands = commands.stream().distinct().toList();

        return new AddCommandsCollected(commands, oldNewWayPairs, failedNodeIds);
    }

    /**
     * 汇总全部移除指令
     * @param ds 数据库
     * @param oldNewWayPairs 新旧路径对（用于执行替换）
     * @return 移除所需的指令
     */
    public List<Command> concludeRemoveCommands(
            DataSet ds, Map<Way, Way> oldNewWayPairs
    ) {
        List<Command> commands = new ArrayList<>();
        for (Map.Entry<Way, Way> oldNewWayEntry: oldNewWayPairs.entrySet()) {
            Way oldWay = oldNewWayEntry.getKey();
            Way newWay = oldNewWayEntry.getValue();
            if (oldWay == null)
                throw new ColumbinaException(
                        I18n.tr("Internal error occurred while removing an old way:\n\n")
                                + I18n.tr("Old way return value is abnormal (null), unable to get the old way.\n\n")
                                + I18n.tr("This way may not have been properly rounded or removed.")
                        // "移除某条旧路径时产生了内部错误：\n\n"
                        //         + "旧路径返回值异常（null），无法获取旧路径。"
                        //         + "该路径可能未被正确倒角或移除。"
                );
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
                List<Command> cmdRmv = UtilsData.getRemoveCmd(ds, oldWay, newWay);
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

    /**
     * 点击事件：绘制数据通用动作模板
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer layer; DataSet dataset; List<Way> selectedWays;
        boolean deleteOld; boolean selectNew; boolean copyTag;

        // 检查
        try {
            final LayerDatasetAndWaySelected lyDsWs = UtilsData.getLayerDatasetAndWaySelected();
            if (lyDsWs == null) return;  // 用户取消操作
            layer = lyDsWs.layer;
            dataset = lyDsWs.dataset;
            selectedWays = lyDsWs.selectedWays;

            // 弹出参数设置窗口，获取参数
            final ParamType params = preference.getParams();  // 重构后preference负责弹窗，本来也就是设置preference的窗口
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
        AddCommandsCollected cmdsAddAndWayPairs;
        List<Command> cmdsAdd;
        Map<Way, Way> oldNewWayPairs;
        Map<Way, List<Long>> failedNodeIds;
        try {
            cmdsAddAndWayPairs = concludeAddCommands(
                    dataset, selectedWays,
                    copyTag
            );
            cmdsAdd = cmdsAddAndWayPairs.commands;
            oldNewWayPairs = cmdsAddAndWayPairs.oldNewWayPairs;
            failedNodeIds = cmdsAddAndWayPairs.failedNodeIds;
        } catch (ColumbinaException | IllegalArgumentException exAdd) {
            UtilsUI.errorInfo(exAdd.getMessage());
            return;
        }

        // 绘制部分的撤销重做栈处理并正式提交执行
        if (!cmdsAdd.isEmpty()) {
            Command cmdAdd = new ColumbinaSeqCommand(getUndoRedoInfo(selectedWays, params), cmdsAdd, "RoundCorners");
            UndoRedoHandler.getInstance().add(cmdAdd);
        }

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

        // 移除旧路径
        if (deleteOld) {
            try {
                List<Command> cmdsRmv = concludeRemoveCommands(dataset, oldNewWayPairs);
                if (!cmdsRmv.isEmpty()) {  // 如果全部都没有删除/替换，cmdsRmv为空会错错爆;
                    Command cmdRmv = new ColumbinaSeqCommand(I18n.tr("Columbina: Remove original ways"), cmdsRmv, "RemoveOldWays");
                    UndoRedoHandler.getInstance().add(cmdRmv);
                }
            } catch (ColumbinaException | IllegalArgumentException | ReplaceGeometryException exRemove) {
                UtilsUI.warnInfo(exRemove.getMessage());
                // return;
            }
            // TODO:选中的旧路径之间有交点且不与其他路径连接时，因为现在删/换数条线是打包到一次undoRedo中同时操作。
            //  删way1时认为交点在way2上，way2认为在way1上，一起做这些交点不会被删除/替换，需要手动处理。
        }

        // 选中新路径
        if (selectNew) {
            List<Way> newWays = new ArrayList<>();
            for (Map.Entry<Way, Way> oldNewWayEntry: oldNewWayPairs.entrySet()) newWays.add(oldNewWayEntry.getValue());
            dataset.setSelected(newWays);
        }
    }
}
