package yakxin.columbina;

import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Map;

import javax.swing.*;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;
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
                "Columbina",
                BUTTON_TEXTS,
                true);

        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 窗体
        header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ImageIcon icon = new ImageProvider("RoundCorners").setSize(24,24).get();
        header.add(new JLabel(icon));
        header.add(new JLabel("<html><h1 style=\"font-size:10px;\">路径倒圆角</h1></html>"));
        panel.add(header, GBC.eol().insets(0, 0, 0, 0).fill(GridBagConstraints.HORIZONTAL));

        sectionCurveInfo = utils.addSection(panel, "曲线信息");
        filletR = utils.addInput(panel, "倒角半径（m）：", String.valueOf(Preference.getRadius()));
        filletAngleStep = utils.addInput(panel, "新曲线圆心角步进（角度°）：", String.valueOf(Preference.getAngleStep()));
        utils.addLabel(panel, "<html><div style=\"width:275\">※　用于指定曲线的平滑度，步进越小，产生的点越密集，曲线越平滑。</div></html>", 15);
        filletMaxPointNum = utils.addInput(panel, "每段曲线最大点数（首尾不计）：", String.valueOf(Preference.getMaxPointPerArc()));
        utils.addSpace(panel,4);
        minAngleDeg = utils.addInput(panel, "允许绘制曲线的最小张角（角度°）：", String.valueOf(Preference.getMinAngleDeg()));
        maxAngleDeg = utils.addInput(panel, "允许绘制曲线的最大张角（角度°）：", String.valueOf(Preference.getMaxAngleDeg()));
        utils.addLabel(panel, "<html><div style=\"width:275\">※　张角接近于0°时将形成发卡角，接近于180°时表明这个拐角附近线条已经较为平滑，此二种情况多数不需要圆角。</div></html>", 15);

        sectionOptionInfo = utils.addSection(panel, "其他操作");
        copyTag = utils.addCheckbox(panel, "复制原有路径标签", Preference.isCopyTag());
        deleteOldWays = utils.addCheckbox(panel, "绘制后移除原有路径", Preference.isDeleteOldWays());
        selectNewWays = utils.addCheckbox(panel, "绘制后切换选择新路径", Preference.isSelectNewWays());

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
            return Math.max(0.0, NumberFormat.getInstance().parse(minAngleDeg.getText()).doubleValue());
        } catch (ParseException e) {
            return Preference.DEFAULT_MIN_ANGLE_DEG;
            // 最小、最大允许张角范围外会强制0°、180°
        }
    }
    public double getMaxAngleDeg() {
        try {
            return Math.min(180.0, NumberFormat.getInstance().parse(maxAngleDeg.getText()).doubleValue());
        } catch (ParseException e) {
            return Preference.DEFAULT_MAX_ANGLE_DEG;
        }
    }
    public boolean getIfCopyTag() {return copyTag.isSelected();}
    public boolean getIfDeleteOld() {return deleteOldWays.isSelected();}
    public boolean getIfSelectNew() {return selectNewWays.isSelected();}
}


