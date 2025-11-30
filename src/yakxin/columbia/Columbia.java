package yakxin.columbia;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import javax.swing.*;
import java.awt.event.ActionEvent;

// %APPDATA%\JOSM\plugins\
public class Columbia extends Plugin {

    public Columbia(PluginInformation info) {
        super(info);

        JMenuItem item = new JMenuItem(new AbstractAction("Hello JOSM") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Hello, JOSM!",
                        "Columbia",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
        MainApplication.getMenu().moreToolsMenu.add(item);
    }
}


