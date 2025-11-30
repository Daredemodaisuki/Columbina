package yakxin.columbia;

import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.GBC;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;

/// 倒圆角对话框
public class RoundCornersDialog extends ExtendedDialog {
    private static final String[] BUTTEON_TEXTS = new String[] {"确定", "取消"};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    protected final JPanel panel = new JPanel(new GridBagLayout());
    private JFormattedTextField filletR;
    private JCheckBox deleteOldWays;
    private JCheckBox selectNewWays;

    // 构建窗口
    protected RoundCornersDialog() {
        // 标题、按钮
        super(MainApplication.getMainFrame(),
                "路径倒圆角",
                BUTTEON_TEXTS,
                true);

        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 窗体
        filletR = utils.addInput(panel, "倒角半径（m）：", "100");
        deleteOldWays = utils.addCheckbox(panel, "绘制后移除原有路径", false);
        selectNewWays = utils.addCheckbox(panel, "绘制后切换选择新路径", true);

        contentInsets = new Insets(15, 15, 5, 15);  // 内容边距
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
            return 0;
        }
    }

}
