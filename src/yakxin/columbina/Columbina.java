package yakxin.columbina;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

// %APPDATA%\JOSM\plugins\
public class Columbina extends Plugin {

    public Columbina(PluginInformation info) {
        super(info);

        // 注册菜单
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new RoundCornersAction());  // 倒圆角
        // TODO：检查拐点两侧是否基本没弯
        // TODO：检查结果中曲线错开导致的尖角
        // TODO：实现一个统一的异常类及其处理
    }
}


