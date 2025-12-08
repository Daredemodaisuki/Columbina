package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.AbstractDrawingAction;

import java.awt.event.KeyEvent;
import java.util.List;

public final class AngleLineAction extends
        AbstractDrawingAction<AngleLineGenerator, AngleLinePreference, AngleLineParams, Node>
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
    public AngleLineAction(String name, String iconName, String description, Shortcut shortcut, AngleLineGenerator generator, AngleLinePreference preference) {
        super(
                name, iconName, description, shortcut,
                generator, preference, Node.class,
                -1, 1
        );
    }

    /**
     * 懒得在主类填参数就用静态工厂方法
     * @return 构建好的实例
     */
    public static AngleLineAction create() {
        return new AngleLineAction(
                I18n.tr("Oriented Line"), "OrientedLine",
                I18n.tr("Chamfer corners of selected ways with specified distances or angle."),
                Shortcut.registerShortcut(
                        "tools:orientedLine",
                        "More tools: Columbina/Oriented line",
                        KeyEvent.VK_F,
                        Shortcut.ALT_CTRL_SHIFT
                ),
                new AngleLineGenerator(),
                new AngleLinePreference()
        );
    }

    @Override
    public String getUndoRedoInfo(List<Node> selections, AngleLineParams params) {
        return I18n.tr("");
    }
}
