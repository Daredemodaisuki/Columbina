package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.features.angleLine.AngleLinePreference;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CurveConnectDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final JFormattedTextField curveConnectR;
    private final JFormattedTextField cueveConnectTransArcLength;
    private final JFormattedTextField curveConnectChainageLength;
    private final JFormattedTextField curveConnectMaxPointNum;
    private final UtilsUI.RadioButtonGroup leftRight;

    private final JCheckBox selectNewWays;

    CurveConnectDialog() {
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true
        );
        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 单选框组
        leftRight = new UtilsUI.RadioButtonGroup(
                new ArrayList<>(List.of(new String[]{I18n.tr("Turn left"), I18n.tr("Turn right")})),
                CurveConnectPreference.getCurveConnectLeftRight()
        );

        // 窗体
        UtilsUI.addHeader(panel, I18n.tr("Curve Connect"), null);  // 暂不设置图标

        UtilsUI.addSection(panel, I18n.tr("Curve Information"));

        UtilsUI.addLabel(panel, "Curve direction: ");
        leftRight.addRadioButton(panel, CurveConnectGenerator.LEFT);
        leftRight.addRadioButton(panel, CurveConnectGenerator.RIGHT);

        UtilsUI.addSpace(panel, 5);
        curveConnectR = UtilsUI.addInput(panel, I18n.tr("Circular curve surfaceRadius (m): "), CurveConnectPreference.getCurveConnectRadius());
        cueveConnectTransArcLength = UtilsUI.addInput(panel, I18n.tr("Transition curve length (m): "), CurveConnectPreference.getCurveConnectTransArcLength());
        curveConnectChainageLength = UtilsUI.addInput(panel, I18n.tr("Chainage length (node spacing, m): "), CurveConnectPreference.getCurveConnectChainageLength());
        curveConnectMaxPointNum = UtilsUI.addInput(panel, I18n.tr("Maximum nodes (excluding start and end): "), CurveConnectPreference.getCurveConnectMaxPointPerArc());

        UtilsUI.addSection(panel, I18n.tr("Other Operations"));
        selectNewWays = UtilsUI.addCheckbox(panel, I18n.tr("Select new ways after drawing"), AngleLinePreference.isAngleLineSelectNewWays());

        contentInsets = new Insets(5, 15, 5, 15);  // 内容边距
        setContent(panel);

        // 显示
        setupDialog();
        showDialog();
    }

    // 获取数据
    public double getCurveConnectR() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(curveConnectR.getText()).doubleValue();
        } catch (ParseException e) {
            return CurveConnectPreference.DEFAULT_CURVE_CONNECT_RADIUS;
            // 能返回数值就返回，有异常的话返回默认值（但这里不做数值校验，在Action类中检查是否合法）
        }
    }
    public double getCurveConnectTransArcLength() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(cueveConnectTransArcLength.getText()).doubleValue();
        } catch (ParseException e) {
            return CurveConnectPreference.DEFAULT_CURVE_CONNECT_TRANS_ARC_LENGTH;
        }
    }
    public double getCurveConnectChainageLength() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(curveConnectChainageLength.getText()).doubleValue();
        } catch (ParseException e) {
            return CurveConnectPreference.DEFAULT_CURVE_CONNECT_CHAINAGE_LENGTH;
        }
    }
    public int getCurveConnectMaxPointNum() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(curveConnectMaxPointNum.getText()).intValue();
        } catch (ParseException e) {
            return CurveConnectPreference.DEFAULT_CURVE_CONNECT_MAX_POINT_PER_ARC;
        }
    }
    public int getLeftRight() { return leftRight.getButtonSelected();}
    public boolean getIfSelectNew() {return selectNewWays.isSelected();}
}
