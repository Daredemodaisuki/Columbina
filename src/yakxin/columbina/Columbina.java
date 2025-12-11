package yakxin.columbina;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import yakxin.columbina.features.angleLine.AngleLineAction;
import yakxin.columbina.features.chamfer.ChamferAction;
import yakxin.columbina.features.fillet.FilletAction;
import yakxin.columbina.features.transitionCurve.TransitionCurveAction;

// %APPDATA%\JOSM\plugins\
public class Columbina extends Plugin {

    public Columbina(PluginInformation info) {
        super(info);
        // Corner-Optimal Line Utility Modifier for Better Inflection and Node Adjustment
        // 原Cartographic-Oriented Line Utility Modifier with Bevel through Interpolated Node Automation
        // TODO：检查结果中曲线错开导致的尖角

        // 注册菜单
        MainApplication.getMenu().moreToolsMenu.addSeparator();  // 分隔线
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, FilletAction.create());  // 倒圆角
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, ChamferAction.create());  // 倒斜角
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, TransitionCurveAction.create());  // 过渡曲线

        MainMenu.add(MainApplication.getMenu().moreToolsMenu, AngleLineAction.create());
    }
}


