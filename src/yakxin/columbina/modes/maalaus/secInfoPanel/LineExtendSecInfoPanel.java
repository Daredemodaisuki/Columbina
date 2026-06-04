package yakxin.columbina.modes.maalaus.secInfoPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import yakxin.columbina.data.dto.modelsDTO.maalaus.LineExtendDisplayData;
import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;
import yakxin.columbina.utils.UtilsUI;

/**
 * 直线延伸模式的曲段信息面板
 * <p>显示当前待确认直线段的朝向（Bearing）和长度（Length），
 * 跟随鼠标实时刷新。使用输入框显示数值，初始值为 0。
 * INFO 状态下用户编辑输入框时，通过 InputChangeListener 回调上报数据。
 */
public class LineExtendSecInfoPanel implements SecInfoPanel {

    private final JPanel panel;
    private final JTextField bearingField;
    private final JTextField lengthField;
    private InputChangeListener inputChangeListener;
    private boolean programmaticUpdate = false;

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

        // 注册输入变更监听
        DocumentListener docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onFieldChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onFieldChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onFieldChanged(); }
        };
        bearingField.getDocument().addDocumentListener(docListener);
        lengthField.getDocument().addDocumentListener(docListener);
        // TODO：需要在卸载面板时移除监听器？
    }

    private JTextField createValueField() {
        JTextField field = new JTextField("0");
        field.setEditable(false);
        field.setForeground(new Color(255, 255, 200));
        field.setFont(new Font("SansSerif", Font.PLAIN, 10));
        field.setBackground(new Color(70, 70, 70));
        field.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 3, 1, 3));
        field.setCaretColor(new Color(255, 255, 200));
        field.setPreferredSize(new Dimension(80, 18));
        return field;
    }

    /**
     * 输入框内容变更时，解析数值并回调监听器
     */
    private void onFieldChanged() {
        if (programmaticUpdate) return;
        if (inputChangeListener == null) return;

        Double bearing = parseDouble(bearingField.getText());
        Double length = parseDouble(lengthField.getText());
        if (bearing == null || length == null) return;

        inputChangeListener.onInputChanged(new LineExtendDisplayData(bearing, length));
    }

    /**
     * 从输入框文本中解析数值，去除单位后缀（°、m 等）
     * @return 解析成功返回数值，失败返回 null
     */
    private static Double parseDouble(String text) {
        if (text == null || text.isBlank()) return null;
        // 去除尾部非数字非小数点非负号字符（如 °、m、空格）
        String cleaned = text.trim().replaceAll("[^0-9.\\-].*$", "");
        if (cleaned.isEmpty()) return null;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void updateValues(SecDisplayData data) {
        if (data instanceof LineExtendDisplayData) {
            LineExtendDisplayData led = (LineExtendDisplayData) data;
            programmaticUpdate = true;
            bearingField.setText(String.format("%.1f°", led.bearingDeg));
            lengthField.setText(String.format("%.1f m", led.lengthM));
            programmaticUpdate = false;
        }
    }

    @Override
    public void setEditable(boolean editable) {
        bearingField.setEditable(editable);
        lengthField.setEditable(editable);
    }

    @Override
    public void setInputChangeListener(InputChangeListener listener) {
        this.inputChangeListener = listener;
    }

    @Override
    public void requestFieldFocus() {
        // 将焦点请求延迟到事件队列的尾部，确保窗口完全显示后再请求焦点
        // 引用等价于() -> bearingField.requestFocusInWindow()
        SwingUtilities.invokeLater(bearingField::requestFocusInWindow);
    }
}
