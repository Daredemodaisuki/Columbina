package yakxin.columbina.transitionCurve;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.dto.PanelSectionResult;
import yakxin.columbina.data.preference.TransitionCurvePreference;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * 过渡曲线（缓和曲线）对话框
 */
public class TransitionCurveDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {
            I18n.tr("Confirm"), I18n.tr("Cancel")
    };
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    protected final JPanel panel = new JPanel(new GridBagLayout());
    private final JPanel header;
    private final PanelSectionResult sectionCurveInfo;
    private final JFormattedTextField transitionRadius;
    private final JFormattedTextField transitionLength;
    private final JFormattedTextField chainageLength;

    private final PanelSectionResult sectionOptionInfo;
    private final JCheckBox deleteOldWays;
    private final JCheckBox selectNewWays;
    private final JCheckBox copyTag;

    // 构建窗口
    public TransitionCurveDialog() {
        // 标题、按钮
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);

        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 窗体
        header = UtilsUI.addHeader(panel, I18n.tr("Transition Curve"), null);  // 暂不设置图标

        sectionCurveInfo = UtilsUI.addSection(panel, I18n.tr("Curve Parameters"));
        transitionRadius = UtilsUI.addInput(
                panel,
                I18n.tr("Circular curve radius (m):"),
                String.valueOf(TransitionCurvePreference.getTransitionCurveRadius())
        );
        UtilsUI.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ Radius of the circular arc section. Must be positive.")
                        + "</div></html>",
                15
        );

        transitionLength = UtilsUI.addInput(
                panel,
                I18n.tr("Transition curve length (m):"),
                String.valueOf(TransitionCurvePreference.getTransitionCurveLength())
        );
        UtilsUI.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ Length of each Euler spiral (transition curve) segment.")
                        + "</div></html>",
                15
        );

        chainageLength = UtilsUI.addInput(
                panel,
                I18n.tr("Chainage length (node spacing, m):"),
                String.valueOf(TransitionCurvePreference.getTransitionChainageLength())
        );
        UtilsUI.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ Distance between nodes along the curve. Smaller values create smoother curves but more nodes.")
                        + "</div></html>",
                15
        );

        sectionOptionInfo = UtilsUI.addSection(panel, I18n.tr("Other Operations"));
        copyTag = UtilsUI.addCheckbox(
                panel,
                I18n.tr("Copy original ways'' tags"),
                TransitionCurvePreference.isTransitionCopyTag()
        );
        deleteOldWays = UtilsUI.addCheckbox(
                panel,
                I18n.tr("Remove original ways after drawing"),
                TransitionCurvePreference.isTransitionDeleteOldWays()
        );
        selectNewWays = UtilsUI.addCheckbox(
                panel,
                I18n.tr("Select new ways after drawing"),
                TransitionCurvePreference.isTransitionSelectNewWays()
        );

        contentInsets = new Insets(5, 15, 5, 15);  // 内容边距
        setContent(panel);

        // 显示
        setupDialog();
        showDialog();
    }

    // 获取数据
    public double getTransitionRadius() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(transitionRadius.getText()).doubleValue();
        } catch (ParseException e) {
            return TransitionCurvePreference.DEFAULT_TRANSITION_CURVE_RADIUS;
        }
    }

    public double getTransitionLength() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(transitionLength.getText()).doubleValue();
        } catch (ParseException e) {
            return TransitionCurvePreference.DEFAULT_TRANSITION_CURVE_LENGTH;
        }
    }

    public double getChainageLength() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(chainageLength.getText()).doubleValue();
        } catch (ParseException e) {
            return TransitionCurvePreference.DEFAULT_TRANSITION_CHAINAGE_LENGTH;
        }
    }

    public boolean getIfCopyTag() {
        return copyTag.isSelected();
    }

    public boolean getIfDeleteOld() {
        return deleteOldWays.isSelected();
    }

    public boolean getIfSelectNew() {
        return selectNewWays.isSelected();
    }
}