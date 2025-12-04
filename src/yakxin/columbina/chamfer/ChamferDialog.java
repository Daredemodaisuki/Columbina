package yakxin.columbina.chamfer;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.preference.ChamferPreference;
import yakxin.columbina.data.preference.FilletPreference;
import yakxin.columbina.data.dto.PanelSectionResult;
import yakxin.columbina.utils;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * 倒斜角对话框
 */
public class ChamferDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    protected final JPanel panel = new JPanel(new GridBagLayout());
    private final JPanel header;
    private final PanelSectionResult sectionChamferInfo;
    private final JFormattedTextField chamferDistanceA;
    private final JFormattedTextField chamferDistanceB;
    private final JFormattedTextField chamferAngleADeg;

    private final PanelSectionResult sectionOptionInfo;
    private final JCheckBox deleteOldWays;
    private final JCheckBox selectNewWays;
    private final JCheckBox copyTag;

    // 构建窗口
    protected ChamferDialog() {
        // 标题、按钮
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);
        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 窗体
        header = utils.addHeader(panel, I18n.tr("Chamfer Corners"), null);  // 暂不设置图标

        sectionChamferInfo = utils.addSection(panel, I18n.tr("Chamfer Information"));
        chamferDistanceA = utils.addInput(panel, I18n.tr("Chamfer distance A (m):"), String.valueOf(ChamferPreference.getChamferDistanceA()));
        chamferDistanceB = utils.addInput(panel, I18n.tr("Chamfer distance B (m):"), String.valueOf(ChamferPreference.getChamferDistanceB()));
        chamferAngleADeg = utils.addInput(panel, I18n.tr("Chamfer angle to A (m):"), String.valueOf(ChamferPreference.getChamferAngleADeg()));

        sectionOptionInfo = utils.addSection(panel, I18n.tr("Other Operations"));
        copyTag = utils.addCheckbox(panel, I18n.tr("Copy original ways'' tags"), ChamferPreference.isChamferCopyTag());
        deleteOldWays = utils.addCheckbox(panel, I18n.tr("Remove original ways after drawing"), ChamferPreference.isChamferDeleteOldWays());
        selectNewWays = utils.addCheckbox(panel, I18n.tr("Select new ways after drawing"), ChamferPreference.isChamferSelectNewWays());

        contentInsets = new Insets(5, 15, 5, 15);  // 内容边距
        setContent(panel);

        // 显示
        setupDialog();
        showDialog();
    }

    // 获取数据
    public double getChamferDistanceA() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(chamferDistanceA.getText()).doubleValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_RADIUS;
            // 能返回数值就返回，有异常的话返回默认值（但这里不做数值校验，在Action类中检查是否合法）
        }
    }
    public double getChamferDistanceB() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(chamferDistanceB.getText()).doubleValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_RADIUS;
        }
    }
    public double getChamferAngleADeg() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(chamferAngleADeg.getText()).doubleValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_RADIUS;
        }
    }
    public boolean getIfCopyTag() {return copyTag.isSelected();}
    public boolean getIfDeleteOld() {return deleteOldWays.isSelected();}
    public boolean getIfSelectNew() {return selectNewWays.isSelected();}
}


