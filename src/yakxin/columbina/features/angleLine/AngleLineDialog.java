package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.dto.PanelSectionResult;
import yakxin.columbina.data.inputs.ColumbinaInput;
import yakxin.columbina.features.fillet.FilletPreference;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class AngleLineDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    protected final JPanel panel = new JPanel(new GridBagLayout());
    private final JPanel header;
    private final PanelSectionResult sectionLineInfo;
    private final JFormattedTextField angleLineAngleDeg;
    private final JFormattedTextField angleLineLength;

    private final PanelSectionResult sectionOptionInfo;
    private final JCheckBox selectNewWays;

    protected AngleLineDialog() {
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);

        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 窗体
        header = UtilsUI.addHeader(panel, I18n.tr("Oriented Line"), "OrientedLine");

        sectionLineInfo = UtilsUI.addSection(panel, I18n.tr("Line Information"));
        angleLineAngleDeg = UtilsUI.addInput(panel, I18n.tr("Angle (degrees°): "));
        angleLineLength = UtilsUI.addInput(panel, I18n.tr("Length (m): "));

        sectionOptionInfo = UtilsUI.addSection(panel, I18n.tr("Other Operations"));
        selectNewWays = UtilsUI.addCheckbox(panel, I18n.tr("Select new ways after drawing"), FilletPreference.isFilletSelectNewWays());

        contentInsets = new Insets(5, 15, 5, 15);  // 内容边距
        setContent(panel);

        // 显示
        setupDialog();
        showDialog();
    }

    // 获取数据
    public double getAngleLineAngleDeg() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(angleLineAngleDeg.getText()).doubleValue();
        } catch (ParseException e) {
            return AngleLinePreference.DEFAULT_ANGLE_LINE_ANGLE_DEG;
            // 能返回数值就返回，有异常的话返回默认值（但这里不做数值校验，在Action类中检查是否合法）
        }
    }
    public double getAngleLineLength() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(angleLineLength.getText()).doubleValue();
        } catch (ParseException e) {
            return AngleLinePreference.DEFAULT_ANGLE_LINE_LENGTH;
        }
    }
    public boolean getIfSelectNew() {return selectNewWays.isSelected();}
}
