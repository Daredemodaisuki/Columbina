package yakxin.columbia;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.GBC;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

public class utils {
    protected static void testMsgWindow(String info) {
        JOptionPane.showMessageDialog(
                null,
                info,
                "调试输出",
                JOptionPane.INFORMATION_MESSAGE);
    }

    static JFormattedTextField addInput(JPanel panel, String labelText) {
        return addInput(panel, labelText, "");
    }
    static JFormattedTextField addInput(JPanel panel, String labelText, String initInput) {
        JFormattedTextField input = new JFormattedTextField(NumberFormat.getInstance());
        JLabel label = new JLabel(labelText);
        panel.add(label, GBC.std());
        label.setLabelFor(input);
        panel.add(input, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        input.setText(initInput);  // 初始值
        return input;
    }

    static JCheckBox addCheckbox(JPanel panel, String labelText, boolean initialCheck) {
        JCheckBox checkbox = new JCheckBox(labelText);
        panel.add(checkbox, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        checkbox.setSelected(initialCheck);
        return checkbox;
    }

    static void wayReplaceNode(Way way, int index, Node newNode) {
        way.removeNode(way.getNode(index));
        way.addNode(index, newNode);
    }
}


