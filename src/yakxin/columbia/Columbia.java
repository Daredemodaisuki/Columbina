package yakxin.columbia;

// import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

// import javax.swing.*;
//import java.awt.event.ActionEvent;

// %APPDATA%\JOSM\plugins\
public class Columbia extends Plugin {

    public Columbia(PluginInformation info) {
        super(info);

        // 添加菜单
        new RoundCornersAction();  // 倒圆角
    }
}


