package yakxin.columbina;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import yakxin.columbina.chamfer.ChamferAction;
import yakxin.columbina.roundCorner.RoundCornersAction;

// %APPDATA%\JOSM\plugins\
public class Columbina extends Plugin {

    public Columbina(PluginInformation info) {
        super(info);
        // Corner-Optimal Line Utility Modifier for Better Inflection and Node Adjustment
        // 原Cartographic-Oriented Line Utility Modifier with Bevel through Interpolated Node Automation

        // 注册菜单
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new RoundCornersAction());  // 倒圆角
        // TODO：检查结果中曲线错开导致的尖角
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new ChamferAction());  // 倒斜角
    }
}


