package yakxin.columbina.utils;

import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import yakxin.columbina.data.dto.PanelSectionResult;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class UtilsUI {
    // 测试用调试输出
    public static void testMsgWindow(String info) {
        JOptionPane.showMessageDialog(
                null,
                info,
                I18n.tr("Debug Output"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /// 窗体相关
    // 左下角警告信息
    public static void warnInfo(String info) {
        (new Notification(I18n.tr("Columbina\n\n") + info)).setIcon(JOptionPane.WARNING_MESSAGE).show();
    }
    // 左下角错误信息
    public static void errorInfo(String info) {
        (new Notification(I18n.tr("Columbina\n\n") + info)).setIcon(JOptionPane.ERROR_MESSAGE).show();
    }

    // 标题栏
    public static JPanel addHeader(JPanel panel, String headTitle, String iconName) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (iconName != null && iconName != "") {
            ImageIcon icon = new ImageProvider(iconName).setSize(24,24).get();
            header.add(new JLabel(icon));
        }
        header.add(new JLabel("<html><h1 style=\"font-size:10px;\">" + headTitle + "</h1></html>"));
        panel.add(header, GBC.eol().insets(0, 0, 0, 0).fill(GridBagConstraints.HORIZONTAL));

        return header;
    }

    // 输入框
    public static JFormattedTextField addInput(JPanel panel, String labelText) {
        return addInput(panel, labelText, "");
    }
    public static JFormattedTextField addInput(JPanel panel, String labelText, String initInput) {
        JFormattedTextField input = new JFormattedTextField(NumberFormat.getInstance(Locale.US));  // 强制美式数码格式，「.」为小数点
        JLabel label = new JLabel(labelText);
        panel.add(label, GBC.std());
        label.setLabelFor(input);
        panel.add(input, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        input.setText(initInput);  // 初始值
        return input;
    }

    // 复选框
    public static JCheckBox addCheckbox(JPanel panel, String labelText, boolean initialCheck) {
        JCheckBox checkbox = new JCheckBox(labelText);
        panel.add(checkbox, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        checkbox.setSelected(initialCheck);
        return checkbox;
    }

    // 标签
    public static JLabel addLabel(JPanel panel, String text) {
        return addLabel(panel, text, 0);
    }
    public static JLabel addLabel(JPanel panel, String text, int leftIndents) {
        UtilsUI.addSpace(panel,2);

        JLabel label = new JLabel(text);
        panel.add(label, GBC.eol().insets(leftIndents, 0, 0, 0).fill(GridBagConstraints.HORIZONTAL));

        UtilsUI.addSpace(panel,2);

        return label;
    }

    // 分隔线
    public static JSeparator addSeparator(JPanel panel) {  // 横向分割线
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        panel.add(sep, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        return sep;
    }

    // 空格
    public static void addSpace(JPanel panel, int height) {
        panel.add(Box.createVerticalStrut(height), GBC.eol());
    }

    // 分组栏
    public static PanelSectionResult addSection(JPanel panel, String title) {
        // Map<String, Object> result = new HashMap<>();

        addSpace(panel, 8);
        JLabel label = addLabel(panel, "<html><b>" + title + "</b></html>");
        JSeparator sep = addSeparator(panel);
        addSpace(panel, 4);

        // result.put("titleLabel", label);
        // result.put("separator", sep);
        return new PanelSectionResult(label, sep);
    }

    /**
     * 单选框组类
     * 需要先创建类的实例，用addRadioButton把button放在窗口正确位置上
     */
    public static class RadioButtonGroup {
        public final List<JRadioButton> buttons;
        public final ButtonGroup group;

        public RadioButtonGroup(List<String> labels, int initChoice) {
            buttons = new ArrayList<>();
            group = new ButtonGroup();
            for (String label : labels) {
                JRadioButton b = new JRadioButton(label);
                buttons.add(b);
                group.add(b);
            }
            buttons.get(initChoice).setSelected(true);  // 默认选中
        }

        public int getButtonSelected() {
            int index = 0;
            for (JRadioButton button : buttons) {
                if (button.isSelected()) return index;
                index += 1;
            }
            return -1;
        }

        // 正式添加单选框至窗体
        public void addRadioButton(JPanel panel, int index) {
            panel.add(
                    buttons.get(index),
                    GBC.eol().insets(0, 0, 0, 0).fill(GridBagConstraints.HORIZONTAL)
            );

        }
    }

    // TODO:将标签和输入框单独写个类
}


