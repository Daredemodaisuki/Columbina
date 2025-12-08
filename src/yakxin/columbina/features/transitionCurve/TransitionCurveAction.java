package yakxin.columbina.features.transitionCurve;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;

import java.awt.event.KeyEvent;
import java.util.*;

/**
 * 过渡曲线（缓和曲线）交互类
 */
public final class TransitionCurveAction extends AbstractDrawingAction
    <TransitionCurveGenerator, TransitionCurvePreference, TransitionCurveParams>
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
    private TransitionCurveAction(String name, String iconName, String description, Shortcut shortcut, TransitionCurveGenerator generator, TransitionCurvePreference preference) {
        super(name, iconName, description, shortcut, generator, preference);
    }

    /**
     * 懒得在主类填参数就用静态工厂方法
     * @return 构建好的实例
     */
    public static TransitionCurveAction create() {
        return new TransitionCurveAction(
                I18n.tr("Transition Curve"), "TransitionCurve",
                I18n.tr("Create Euler spiral transition curves between straight segments."),
                Shortcut.registerShortcut(
                        "tools:transitionCurve",
                        "More tools: Columbina/Transition Curve",
                        KeyEvent.VK_T,
                        Shortcut.ALT_CTRL_SHIFT
                ),
                new TransitionCurveGenerator(),
                new TransitionCurvePreference()
        );
    }

    @Override
    public String getUndoRedoInfo(List<Way> selectedWays, TransitionCurveParams params) {
        String undoRedoInfo;
        if (selectedWays.size() == 1) {
            undoRedoInfo = I18n.tr("Transition curve of way {0}: R={1}m, Ls={2}m",
                    selectedWays.getFirst().getUniqueId(), params.surfaceRadius, params.surfaceTransArcLength);
        } else if (selectedWays.size() <= 5) {
            undoRedoInfo = I18n.tr("Transition curve of way {0}: R={1}m, Ls={2}m",
                    selectedWays.stream().map(Way::getId).toList(), params.surfaceRadius, params.surfaceTransArcLength);
        } else {
            undoRedoInfo = I18n.tr("Transition curve of {0} ways: R={1}m, Ls={2}m",
                    selectedWays.size(), params.surfaceRadius, params.surfaceTransArcLength);
        }
        return undoRedoInfo;
    }
}


