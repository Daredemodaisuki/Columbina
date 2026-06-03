package yakxin.columbina.modes.maalaus.secInfoPanel;

import java.awt.*;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import yakxin.columbina.data.dto.modelsDTO.maalaus.LineExtendDisplayData;
import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;
import yakxin.columbina.utils.UtilsUI;

/**
 * 直线延伸模式的曲段信息面板
 * <p>显示当前待确认直线段的朝向（Bearing）和长度（Length），
 * 跟随鼠标实时刷新。使用输入框显示数值，初始值为 0。
 */
public class LineExtendSecInfoPanel implements SecInfoPanel {

    private final JPanel panel;
    private final JTextField bearingField;
    private final JTextField lengthField;

    public LineExtendSecInfoPanel() {
        panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JLabel bearingName = new JLabel(I18n.tr("Bearing") + ": ");
        bearingName.setForeground(new Color(160, 160, 160));
        bearingName.setFont(new Font("SansSerif", Font.PLAIN, 10));
        bearingField = createValueField();

        JLabel lengthName = new JLabel(I18n.tr("Length") + ": ");
        lengthName.setForeground(new Color(160, 160, 160));
        lengthName.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lengthField = createValueField();

        panel.add(bearingName, GBC.std().insets(2, 0, 0, 0));
        panel.add(bearingField, GBC.eol().fill(GridBagConstraints.HORIZONTAL).weight(1.0, 0.0).insets(0, 0, 0, 0));
        panel.add(lengthName, GBC.std().insets(2, 0, 0, 0));
        panel.add(lengthField, GBC.eol().fill(GridBagConstraints.HORIZONTAL).weight(1.0, 0.0).insets(0, 0, 0, 0));
    }

    private static JTextField createValueField() {
        // TODO：检查输入框可修改性
        JTextField field = new JTextField("0.000");
        field.setEditable(true);
        field.setForeground(new Color(255, 255, 200));
        field.setFont(new Font("SansSerif", Font.PLAIN, 10));
        field.setBackground(new Color(70, 70, 70));
        field.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 3, 1, 3));
        field.setCaretColor(new Color(255, 255, 200));
        field.setPreferredSize(new Dimension(80, 18));
        return field;
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void updateValues(SecDisplayData data) {
        if (data instanceof LineExtendDisplayData) {
            LineExtendDisplayData led = (LineExtendDisplayData) data;
            bearingField.setText(String.format("%.1f°", led.bearingDeg));
            lengthField.setText(String.format("%.1f m", led.lengthM));
            // UtilsUI.testMsgWindow(String.format("%.1f°", led.bearingDeg));
        }
    }

    @Override
    public void setEditable(boolean editable) {
        bearingField.setEditable(editable);
        lengthField.setEditable(editable);
    }
}