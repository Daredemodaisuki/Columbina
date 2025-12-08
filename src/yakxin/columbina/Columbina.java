package yakxin.columbina;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.features.fillet.FilletPreference;
import yakxin.columbina.features.chamfer.ChamferAction;
import yakxin.columbina.features.fillet.FilletAction;
import yakxin.columbina.features.fillet.FilletGenerator;
import yakxin.columbina.features.transitionCurve.TransitionCurveAction;

import java.awt.event.KeyEvent;

// %APPDATA%\JOSM\plugins\
public class Columbina extends Plugin {

    public Columbina(PluginInformation info) {
        super(info);
        // Corner-Optimal Line Utility Modifier for Better Inflection and Node Adjustment
        // 原Cartographic-Oriented Line Utility Modifier with Bevel through Interpolated Node Automation
        // TODO：检查结果中曲线错开导致的尖角

        // 注册菜单
        MainApplication.getMenu().moreToolsMenu.addSeparator();  // 分隔线
        // MainMenu.add(MainApplication.getMenu().moreToolsMenu, new FilletAction());  // 倒圆角
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new FilletAction(
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
        ));  // 倒圆角
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new ChamferAction());  // 倒斜角
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new TransitionCurveAction());  // 过渡曲线
    }
}


