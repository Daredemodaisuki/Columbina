package yakxin.columbina.features.mergeUnusedNodes;

import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.data.dto.featuresDTO.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.featuresDTO.inputs.ColumbinaSingleInput;
import yakxin.columbina.data.dto.featuresDTO.outputs.ColumbinaOutputIntent;
import yakxin.columbina.data.dto.featuresDTO.outputs.ColumbinaSingleOutput;

import java.awt.event.KeyEvent;
import java.util.List;

public class MergeUnusedNodesAction extends
        AbstractDrawingAction<MergeUnusedNodesGenerator, MergeUnusedNodesPreference, MergeUnusedNodesParams>
{

    /**
     * 构造函数
     * @param name        功能名称（I18n后）
     * @param iconName    菜单栏功能图标（I18n后）
     * @param description 功能描述（I18n后）
     * @param shortcut    快捷键
     * @param generator   生成器实例
     * @param preference  首选项实例
     */
    public MergeUnusedNodesAction(String name, String iconName, String description, Shortcut shortcut, MergeUnusedNodesGenerator generator, MergeUnusedNodesPreference preference) {
        super(name, iconName, description, shortcut, generator, preference);
    }

    /**
     * 静态工厂方法
     * @return 构建好的实例
     */
    public static MergeUnusedNodesAction create() {
        return new MergeUnusedNodesAction(
                I18n.tr("Merge Unused Nodes"), "TransitionCurve",
                I18n.tr("Merge existing unused nodes to new nodes"),
                Shortcut.registerShortcut(
                        "tools:mergeUnusedNodes",
                        "More tools: Columbina/Merge Unused Nodes",
                        KeyEvent.VK_M,
                        Shortcut.ALT_CTRL_SHIFT
                ),
                new MergeUnusedNodesGenerator(),
                new MergeUnusedNodesPreference()
        );
    }

    @Override
    public String getUndoRedoInfo(ColumbinaInput inputs, MergeUnusedNodesParams params) {
        return I18n.tr("Merged existing unused nodes to new nodes.");
    }

    @Override
    public List<ColumbinaSingleInput> splitBatchInputs(ColumbinaInput totalInput) {
        return defaultNonBatchSplitInputs(totalInput);
    }

    @Override
    public int checkInputNum(ColumbinaInput totalInput) {
        return CHECK_OK;
    }

    @Override
    public int checkInputDetails(List<ColumbinaSingleInput> list) {
        return CHECK_OK;
    }

    /**
     * 避免父类默认的 Way 类型转换，直接收集意图和代表性要素。
     */
    @Override
    protected void onSingleOutputProcessed(
            ColumbinaSingleInput singleInput, ColumbinaSingleOutput singleOutput,
            List<ColumbinaOutputIntent<?>> intents, boolean copyTag
    ) {
        intents.addAll(singleOutput.outputIntents);
        outputRepresentatives.addAll(singleOutput.representatives);
    }
}