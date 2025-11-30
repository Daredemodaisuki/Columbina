package yakxin.columbia;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

// %APPDATA%\JOSM\plugins\
public class Columbia extends Plugin {

    public Columbia(PluginInformation info) {
        super(info);

        // 注册菜单
        new RoundCornersAction();  // 倒圆角
    }
}


