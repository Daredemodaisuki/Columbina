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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class utils {
    // 测试用调试输出
    protected static void testMsgWindow(String info) {
        JOptionPane.showMessageDialog(
                null,
                info,
                "调试输出",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /// 窗体相关
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

    static JLabel addLabel(JPanel panel, String text) {
        return addLabel(panel, text, 0);
    }
    static JLabel addLabel(JPanel panel, String text, int leftIndents) {
        utils.addSpace(panel,2);

        JLabel label = new JLabel(text);
        panel.add(label, GBC.eol().insets(leftIndents, 0, 0, 0).fill(GridBagConstraints.HORIZONTAL));

        utils.addSpace(panel,2);

        return label;
    }

    static JSeparator addSeparator(JPanel panel) {  // 横向分割线
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        panel.add(sep, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        return sep;
    }

    static void addSpace(JPanel panel, int height) {
        panel.add(Box.createVerticalStrut(height), GBC.eol());
    }

    static Map<String, Object> addSection(JPanel panel, String title) {
        Map<String, Object> result = new HashMap<>();

        addSpace(panel, 8);
        JLabel label = addLabel(panel, "<html><b>" + title + "</b></html>");
        JSeparator sep = addSeparator(panel);
        addSpace(panel, 4);

        result.put("titleLabel", label);
        result.put("separator", sep);
        return result;
    }

    /// 数据相关
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


