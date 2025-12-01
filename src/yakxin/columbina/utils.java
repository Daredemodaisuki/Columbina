package yakxin.columbina;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.GBC;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class utils {
    protected static void testMsgWindow(String info) {
        JOptionPane.showMessageDialog(
                null,
                info,
                "调试输出",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // 左下角警告信息
    protected static void warnInfo(String info) {
        (new Notification("Columbina\n\n" + info)).setIcon(JOptionPane.WARNING_MESSAGE).show();
    }
    // 左下角错误信息
    protected static void errorInfo(String info) {
        (new Notification("Columbina\n\n" + info)).setIcon(JOptionPane.ERROR_MESSAGE).show();
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

    static List<Command> tryGetCommandsFromSeqCmd (SequenceCommand seqCmd) {
        List<Command> commands = new ArrayList<>();
        for (PseudoCommand pc : seqCmd.getChildren()) {
            if (pc instanceof Command) {
                commands.add((Command) pc);
            }
        }
        return commands;
    }
}


