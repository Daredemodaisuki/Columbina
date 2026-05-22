package yakxin.columbina.modes.maalaus.secInfoPanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import yakxin.columbina.data.dto.modelsDTO.maalaus.LineExtendDisplayData;
import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;

/**
 * 直线延伸模式的曲段信息面板
 * <p>显示当前待确认直线段的朝向（Bearing）和长度（Length），
 * 跟随鼠标实时刷新。当前为只读标签模式。
 */
public class LineExtendSecInfoPanel implements SecInfoPanel {

    private final JPanel panel;
    private final JLabel bearingVal;
    private final JLabel lengthVal;

    public LineExtendSecInfoPanel() {
        panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JLabel bearingName = new JLabel(I18n.tr("Bearing") + ": ");
        bearingName.setForeground(new Color(160, 160, 160));
        bearingName.setFont(new Font("SansSerif", Font.PLAIN, 10));
        bearingVal = new JLabel(" ");
        bearingVal.setForeground(new Color(255, 255, 200));
        bearingVal.setFont(new Font("SansSerif", Font.PLAIN, 10));

        JLabel lengthName = new JLabel(I18n.tr("Length") + ": ");
        lengthName.setForeground(new Color(160, 160, 160));
        lengthName.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lengthVal = new JLabel(" ");
        lengthVal.setForeground(new Color(255, 255, 200));
        lengthVal.setFont(new Font("SansSerif", Font.PLAIN, 10));

        panel.add(bearingName, GBC.std().insets(2, 0, 0, 0));
        panel.add(bearingVal, GBC.eol().insets(0, 0, 0, 0));
        panel.add(lengthName, GBC.std().insets(2, 0, 0, 0));
        panel.add(lengthVal, GBC.eol().insets(0, 0, 0, 0));
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void updateValues(SecDisplayData data) {
        if (data instanceof LineExtendDisplayData) {
            LineExtendDisplayData led = (LineExtendDisplayData) data;
            bearingVal.setText(String.format("%.1f°", led.bearingDeg));
            lengthVal.setText(String.format("%.1f m", led.lengthM));
        }
    }

    @Override
    public void setEditable(boolean editable) {
        // 当前只读标签，后续可切换为 JTextField
    }
}