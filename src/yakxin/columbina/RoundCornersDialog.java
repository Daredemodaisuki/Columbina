package yakxin.columbina;

import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Map;

import javax.swing.*;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import yakxin.columbina.data.Preference;

/// 倒圆角对话框
public class RoundCornersDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {"确定", "取消"};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    protected final JPanel panel = new JPanel(new GridBagLayout());
    private final JPanel header;
    private final Map<String, Object> sectionCurveInfo;
    private final JFormattedTextField filletR;
    private final JFormattedTextField filletAngleStep;
    private final JFormattedTextField filletMaxPointNum;
    private final JFormattedTextField minAngleDeg;
    private final JFormattedTextField maxAngleDeg;

    private final Map<String, Object> sectionOptionInfo;
    private final JCheckBox deleteOldWays;
    private final JCheckBox selectNewWays;
    private final JCheckBox copyTag;

    // 构建窗口
    protected RoundCornersDialog() {
        // 标题、按钮
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);

        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 窗体
        header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ImageIcon icon = new ImageProvider("RoundCorners").setSize(24,24).get();
        header.add(new JLabel(icon));
        header.add(new JLabel("<html><h1 style=\"font-size:10px;\">" + I18n.tr("Round Corners") + "</h1></html>"));
        panel.add(header, GBC.eol().insets(0, 0, 0, 0).fill(GridBagConstraints.HORIZONTAL));

        sectionCurveInfo = utils.addSection(panel, I18n.tr("Curve Information"));
        filletR = utils.addInput(panel, I18n.tr("Fillet (round corner) radius (m): "), String.valueOf(Preference.getRadius()));
        filletAngleStep = utils.addInput(panel, I18n.tr("Angle step for new curve (degrees°): "), String.valueOf(Preference.getAngleStep()));
        utils.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ Specifies the smoothness of the curve. The smaller the step, the denser the points and the smoother the curve.")
                        + "</div></html>",
                15
        );
        filletMaxPointNum = utils.addInput(panel, I18n.tr("Maximum nodes per curve segment (excluding start and end): "), String.valueOf(Preference.getMaxPointPerArc()));
        utils.addSpace(panel,4);
        minAngleDeg = utils.addInput(panel, I18n.tr("Minimum angle allowed for drawing curves (degrees°): "), String.valueOf(Preference.getMinAngleDeg()));
        maxAngleDeg = utils.addInput(panel, I18n.tr("Maximum angle allowed for drawing curves (degrees°): "), String.valueOf(Preference.getMaxAngleDeg()));
        utils.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ When the angle approaches 0°, it forms a hairpin turn; when it approaches 180°, it indicates the lines near the corner are already relatively smooth. ")
                        + I18n.tr("In both cases, rounding is usually unnecessary.")
                        + "</div></html>",
                15
        );

        sectionOptionInfo = utils.addSection(panel, I18n.tr("Other Operations"));
        copyTag = utils.addCheckbox(panel, I18n.tr("Copy original ways'' tags"), Preference.isCopyTag());
        deleteOldWays = utils.addCheckbox(panel, I18n.tr("Remove original ways after drawing"), Preference.isDeleteOldWays());
        selectNewWays = utils.addCheckbox(panel, I18n.tr("Select new ways after drawing"), Preference.isSelectNewWays());

        contentInsets = new Insets(5, 15, 5, 15);  // 内容边距
        setContent(panel);

        // 显示
        setupDialog();
        showDialog();
    }

    // 获取数据
    public double getFilletRadius() {
        try {
            return NumberFormat.getInstance().parse(filletR.getText()).doubleValue();
        } catch (ParseException e) {
            return Preference.DEFAULT_RADIUS;
            // 能返回数值就返回，有异常的话返回默认值（但这里半径、角度步进、最大点数不做数值校验，在Action类中检查是否合法）
        }
    }
    public double getFilletAngleStep() {
        try {
            return NumberFormat.getInstance().parse(filletAngleStep.getText()).doubleValue();
        } catch (ParseException e) {
            return Preference.DEFAULT_ANGLE_STEP;
        }
    }
    public int getFilletMaxPointNum() {
        try {
            return NumberFormat.getInstance().parse(filletMaxPointNum.getText()).intValue();
        } catch (ParseException e) {
            return Preference.DEFAULT_MAX_POINT_PER_ARC;
        }
    }
    public double getMinAngleDeg() {
        try {
            return NumberFormat.getInstance().parse(minAngleDeg.getText()).doubleValue();
        } catch (ParseException e) {
            return Preference.DEFAULT_MIN_ANGLE_DEG;
        }
    }
    public double getMaxAngleDeg() {
        try {
            return NumberFormat.getInstance().parse(maxAngleDeg.getText()).doubleValue();
        } catch (ParseException e) {
            return Preference.DEFAULT_MAX_ANGLE_DEG;
        }
    }
    public boolean getIfCopyTag() {return copyTag.isSelected();}
    public boolean getIfDeleteOld() {return deleteOldWays.isSelected();}
    public boolean getIfSelectNew() {return selectNewWays.isSelected();}

    public void setMinAngleDeg(double angleDeg) {minAngleDeg.setText(String.valueOf(angleDeg));}
    public void setMaxAngleDeg(double angleDeg) {maxAngleDeg.setText(String.valueOf(angleDeg));}
    public void setFilletAngleStep(double angleStep) {filletAngleStep.setText(String.valueOf(angleStep));}
}


