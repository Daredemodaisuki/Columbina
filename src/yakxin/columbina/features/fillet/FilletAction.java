package yakxin.columbina.features.fillet;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.abstractClasses.actionMiddle.ActionWithBatchWays;
import yakxin.columbina.data.inputs.ColumbinaInput;

import java.awt.event.KeyEvent;

public final class FilletAction extends
        ActionWithBatchWays<FilletGenerator, FilletPreference, FilletParams>
{
    /**
     * 构造函数
     *
     * @param name        功能名称（I18n后）
     * @param iconName    菜单栏功能图标（I18n后）
     * @param description 功能描述（I18n后）
     * @param shortcut    快捷键
     * @param generator   生成器实例
     * @param preference  首选项实例
     */
    private FilletAction(String name, String iconName, String description, Shortcut shortcut, FilletGenerator generator, FilletPreference preference) {
        super(
                name, iconName, description, shortcut,
                generator, preference,
                AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM, AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM
        );
    }

    /**
     * 懒得在主类填参数就用静态工厂方法
     * @return 构建好的实例
     */
    public static FilletAction create() {
        return new FilletAction(
                I18n.tr("Round Corners"), "RoundCorners",
                I18n.tr("Round corners of selected ways with specified surfaceRadius."),
                Shortcut.registerShortcut(
                        "tools:filletCorners",
                        "More tools: Columbina/Round corners",
                        KeyEvent.VK_C,
                        Shortcut.ALT_CTRL_SHIFT
                ),
                new FilletGenerator(),
                new FilletPreference()
        );
    }

    @Override
    public String getUndoRedoInfo(ColumbinaInput inputs, FilletParams params) {
        String undoRedoInfo;
        if (inputs.getInputNum(Way.class) == 1) undoRedoInfo = I18n.tr("Round corners of way {0}: {1}m", inputs.getWays().getFirst().getUniqueId(), params.surfaceRadius);
        else if (inputs.getInputNum(Way.class) <= 5) undoRedoInfo = I18n.tr("Round corners of way {0}: {1}m", inputs.getWays().stream().map(Way::getUniqueId).toList(), params.surfaceRadius);
        else undoRedoInfo = I18n.tr("Round corners of {0} ways: {1}m", inputs.getInputNum(Way.class), params.surfaceRadius);
        return undoRedoInfo;
    }
}


