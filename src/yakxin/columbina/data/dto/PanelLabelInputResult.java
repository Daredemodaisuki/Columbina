package yakxin.columbina.data.dto;

import org.openstreetmap.josm.tools.GBC;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 输入框和配套标签的打包，用于整个隐藏或禁用。
 */
public final class PanelLabelInputResult {
    public JLabel label;
    public JFormattedTextField textField;

    public PanelLabelInputResult(String labelText, double doubleInput) {this(labelText, String.valueOf(doubleInput));}
    public PanelLabelInputResult(String labelText, int intInput) {this(labelText, String.valueOf(intInput));}
    public PanelLabelInputResult(String labelText, String initInput) {
        this.textField = new JFormattedTextField(NumberFormat.getInstance(Locale.US));  // 强制美式数码格式，「.」为小数点
        JLabel label = new JLabel(labelText);
        label.setLabelFor(this.textField);
        this.textField.setText(initInput);  // 初始值
    }
    public PanelLabelInputResult(JLabel label, JFormattedTextField textField) {
        this.label = label;
        this.textField = textField;
    }

    public void setEnabled(boolean enabled) {
        label.setEnabled(enabled);
        textField.setEnabled(enabled);
    }

    public void setVisible(boolean visible) {
        label.setVisible(visible);
        textField.setVisible(visible);
    }

    public String getText() {
        return textField.getText();
    }
}
