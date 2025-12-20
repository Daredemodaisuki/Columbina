package yakxin.columbina.features.chamfer;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 倒斜角对话框
 */
public final class ChamferDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};
    
    // 窗体组件
    private final UtilsUI.RadioButtonGroup modeGroup;
    
    private final JFormattedTextField chamferDistanceA;
    private final JFormattedTextField chamferDistanceC;
    private final JFormattedTextField chamferAngleADeg;
    
    private final JCheckBox deleteOldWays;
    private final JCheckBox selectNewWays;
    private final JCheckBox copyTag;
    
    // 构建窗口
    ChamferDialog(ChamferParams savedParams) {
        super(MainApplication.getMainFrame(), I18n.tr("Columbina"), BUTTON_TEXTS, true);
        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消
        
        // 单选框组
        List<String> modeNames = new ArrayList<>();
        modeNames.add(I18n.tr("Distance Mode"));  // 0 ChamferGenerator.USING_DISTANCE
        modeNames.add(I18n.tr("Angle A Mode"));  // 1 ChamferGenerator.ANGLE_A_MODE
        modeGroup = new UtilsUI.RadioButtonGroup(modeNames, savedParams.mode);
        
        // 窗体
        JPanel panel = new JPanel(new GridBagLayout());
        UtilsUI.addHeader(panel, I18n.tr("Chamfer Corners"), "ChamferCorners");
        
        UtilsUI.addSection(panel, I18n.tr("Chamfer Information"));
        modeGroup.addRadioButton(panel, ChamferGenerator.DISTANCE_MODE);
        modeGroup.addRadioButton(panel, ChamferGenerator.ANGLE_A_MODE);
        chamferDistanceA = UtilsUI.addInput(panel, I18n.tr("Chamfer distance A (m): "), savedParams.surfaceDistanceA);
        chamferDistanceC = UtilsUI.addInput(panel, I18n.tr("Chamfer distance C (m): "), savedParams.surfaceDistanceC);
        chamferAngleADeg = UtilsUI.addInput(panel, I18n.tr("Chamfer angle to A (degrees°): "), savedParams.angleADeg);
        
        UtilsUI.addSection(panel, I18n.tr("Other Operations"));
        copyTag = UtilsUI.addCheckbox(panel, I18n.tr("Copy original ways'' tags"), savedParams.copyTag);
        deleteOldWays = UtilsUI.addCheckbox(panel, I18n.tr("Remove original ways after drawing"), savedParams.deleteOld);
        selectNewWays = UtilsUI.addCheckbox(panel, I18n.tr("Select new ways after drawing"), savedParams.selectNew);
        
        contentInsets = new Insets(5, 15, 5, 15);  // 内容边距
        bindModeToInputs(modeGroup, chamferDistanceA, chamferDistanceC, chamferAngleADeg);  // 监听单选框，绑定输入框可用性
        setContent(panel);
        
        // 显示
        setupDialog();
        showDialog();
    }
    
    // 绑定、监听编辑模式切换函数
    public static void bindModeToInputs(UtilsUI.RadioButtonGroup modeGroup,
                                        JFormattedTextField distanceA,
                                        JFormattedTextField distanceC,
                                        JFormattedTextField angleADeg
    ) {
        // 监听到单选框切换时更改可见性
        Runnable update = () -> {
            if (modeGroup.getButtonSelected() == ChamferGenerator.DISTANCE_MODE) {
                distanceA.setEnabled(true); distanceC.setEnabled(true);
                angleADeg.setEnabled(false);
            } else {
                distanceA.setEnabled(true); distanceC.setEnabled(false);
                angleADeg.setEnabled(true);
            }
        };  // TODO:换成隐藏（需要addInput函数把label也甩出来，所以不如封装个类）
        
        modeGroup.buttons.get(0).addItemListener(e -> update.run());
        modeGroup.buttons.get(1).addItemListener(e -> update.run());
        
        // 测试
        // for (JRadioButton button : modeGroup.buttons) {
        //     button.addActionListener(e -> {
        //         UtilsUI.testMsgWindow("Radio button clicked: " + modeGroup.getButtonSelected()); // 调试输出
        //         update.run();
        //     });
        // }
        
        // 初始调用
        update.run();
    }
    
    // 获取数据
    public ChamferParams getParams() {
        return new ChamferParams(
                getChamferMode(),
                getChamferDistanceA(), getChamferDistanceC(),
                getChamferAngleADeg(),
                getIfDeleteOld(), getIfSelectNew(), getIfCopyTag()
        );
    }
    
    public double getChamferDistanceA() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(chamferDistanceA.getText()).doubleValue();
        } catch (ParseException e) {
            return ChamferPreference.DEFAULT_CHAMFER_DISTANCE_A;
            // 能返回数值就返回，有异常的话返回默认值（但这里不做数值校验，在Action类中检查是否合法）
        }
    }
    public double getChamferDistanceC() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(chamferDistanceC.getText()).doubleValue();
        } catch (ParseException e) {
            return ChamferPreference.DEFAULT_CHAMFER_DISTANCE_C;
        }
    }
    public double getChamferAngleADeg() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(chamferAngleADeg.getText()).doubleValue();
        } catch (ParseException e) {
            return ChamferPreference.DEFAULT_CHAMFER_ANGLE_A_DEG;
        }
    }
    public boolean getIfCopyTag() { return copyTag.isSelected(); }
    public boolean getIfDeleteOld() { return deleteOldWays.isSelected(); }
    public boolean getIfSelectNew() { return selectNewWays.isSelected(); }
    public int getChamferMode() { return modeGroup.getButtonSelected(); }
}