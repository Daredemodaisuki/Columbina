package yakxin.columbia;

import org.openstreetmap.josm.tools.GBC;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

public class utils {
    static JFormattedTextField addInput(JPanel panel, String labelText) {
        JFormattedTextField input = new JFormattedTextField(NumberFormat.getInstance());
        JLabel label = new JLabel(labelText);
        panel.add(label, GBC.std());
        label.setLabelFor(input);
        panel.add(input, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        return input;
    }

    static JCheckBox addCheckbox(JPanel panel, String labelText, boolean initialCheck) {
        JCheckBox checkbox = new JCheckBox(labelText);
        panel.add(checkbox, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        checkbox.setSelected(initialCheck);
        return checkbox;
    }
}
