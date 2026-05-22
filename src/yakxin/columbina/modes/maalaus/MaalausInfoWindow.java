package yakxin.columbina.modes.maalaus;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;
import yakxin.columbina.modes.maalaus.secInfoPanel.SecInfoPanel;
import yakxin.columbina.utils.UtilsUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 * Maalaus 模式的信息窗口
 * <p>使用无边框 {@link JWindow}，显示当前子模式、统计信息和操作按钮。
 * 窗口跟随鼠标位置移动。
 */
public class MaalausInfoWindow extends JWindow {

    /**
     * 按钮事件监听接口，供 MaalausMapMode 实现以统一处理用户输入
     */
    public interface ButtonListener {
        void onUndo();
        void onCommit();
        void onCancel();
    }

    private final JLabel modeLabel;
    private final JLabel statusLabel;
    private final JLabel statusInfoLabel;
    private final JLabel countLabel;
    private final JLabel infoLabel;
    private final JButton undoButton;
    private final JButton commitButton;
    private final JButton cancelButton;
    private final JPanel secInfoPlaceholder;
    private SecInfoPanel currentSecInfoPanel;

    /**
     * 构造函数
     */
    public MaalausInfoWindow(ButtonListener listener) {
        setLayout(new BorderLayout());  // JWindow 本身布局保持 BorderLayout 以便放置 mainPanel
        setAlwaysOnTop(true);
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 120, 255), 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        mainPanel.setBackground(new Color(50, 50, 50, 220));
        mainPanel.setOpaque(true);
        
        // 模式名称
        // modeLabel = new JLabel("LINE_EXTEND");
        modeLabel = UtilsUI.addLabel(mainPanel, "LINE_EXTEND");
        modeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        modeLabel.setForeground(Color.WHITE);
        // mainPanel.add(modeLabel, GBC.eol());
        
        // 状态信息面板（FlowLayout横向）
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        infoPanel.setOpaque(false);
        statusLabel = UtilsUI.addLabel(infoPanel, "INIT", GBC.std());
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        countLabel = UtilsUI.addLabel(infoPanel, "Secs: 0");
        countLabel.setForeground(new Color(200, 200, 200));
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mainPanel.add(infoPanel, GBC.eol());
        
        // 提示信息面板
        JPanel infoLabelPanel = new JPanel(new GridBagLayout());
        infoLabelPanel.setOpaque(false);
        infoLabel = UtilsUI.addLabel(infoLabelPanel, " ");
        infoLabel.setForeground(new Color(180, 220, 255));
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        statusInfoLabel = UtilsUI.addLabel(infoLabelPanel, " ");
        statusInfoLabel.setForeground(new Color(180, 220, 255));
        statusInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        mainPanel.add(infoLabelPanel, GBC.eol());

        // 曲段信息子面板占位容器（由子模式通过 SecInfoPanel 自行构建）
        secInfoPlaceholder = new JPanel(new BorderLayout());
        secInfoPlaceholder.setOpaque(false);
        mainPanel.add(secInfoPlaceholder, GBC.eol());

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        buttonPanel.setOpaque(false);
        undoButton = UtilsUI.addButton(buttonPanel, I18n.tr("Undo"), (ActionEvent e) -> listener.onUndo(), GBC.std());
        commitButton = UtilsUI.addButton(buttonPanel, I18n.tr("Finish"), (ActionEvent e) -> listener.onCommit(), GBC.std());
        cancelButton = UtilsUI.addButton(buttonPanel, I18n.tr("Cancel"), (ActionEvent e) -> listener.onCancel());
        UtilsUI.addSpace(mainPanel, 5);
        mainPanel.add(buttonPanel, GBC.eol());
        
        add(mainPanel);
        pack();
        setSize(300, 140);   // 高度适当增大以容纳所有组件
    }
    // ----------------------------------------------------------------
    // 更新方法
    // ----------------------------------------------------------------

    /**
     * 更新模式标签
     * @param modeText 模式文本
     */
    public void updateMode(String modeText) {
        modeLabel.setText(modeText);
        repaint();
    }

    /**
     * 更新状态标签
     * @param stateText 状态文本
     */
    public void updateStatus(String stateText, String statusInfoText) {
        statusLabel.setText(stateText);
        statusInfoLabel.setText(statusInfoText);
        repaint();
    }

    /**
     * 更新曲段计数
     * @param count 曲段数量
     */
    public void updateSecCount(int count) {
        countLabel.setText("Secs: " + count);
        repaint();
    }

    /**
     * 更新当前模式的提示信息
     * @param infoText 提示文本
     */
    public void updateInfo(String infoText) {
        infoLabel.setText(infoText != null ? infoText : " ");
    }

    /**
     * 根据子模式重建曲段信息子面板
     * <p>委托给 {@link SecInfoPanel}，由子模式自行构建面板内容。
     * @param subMode 当前子模式
     */
    public void rebuildSecInfo(MaalausSubMode subMode) {
        secInfoPlaceholder.removeAll();
        currentSecInfoPanel = subMode.createSecInfoPanel();
        if (currentSecInfoPanel != null) {
            currentSecInfoPanel.getPanel().setVisible(true);
            secInfoPlaceholder.add(currentSecInfoPanel.getPanel(), BorderLayout.CENTER);
        }
        secInfoPlaceholder.revalidate();
        secInfoPlaceholder.repaint();
    }

    /**
     * 更新曲段信息子面板
     * <p>直接接收显示数据 DTO 并委托给当前 {@link SecInfoPanel#updateValues(SecDisplayData)}。
     * @param data 显示数据 DTO
     */
    public void updateSecInfoValues(SecDisplayData data) {
        if (currentSecInfoPanel != null && data != null) {
            currentSecInfoPanel.updateValues(data);
        }
    }

    /**
     * 切换曲段信息面板的编辑模式
     * @param editable DRAW 时为 false（只读标签），INFO 时为 true（可输入）
     */
    public void setSecInfoEditable(boolean editable) {
        if (currentSecInfoPanel != null) {
            currentSecInfoPanel.setEditable(editable);
        }
    }

    /**
     * 更新窗口位置到鼠标附近
     * @param screenX 鼠标屏幕 X 坐标
     * @param screenY 鼠标屏幕 Y 坐标
     */
    public void reposition(int screenX, int screenY) {
        setLocation(screenX + 20, screenY - 20);
    }

    // ----------------------------------------------------------------
    // 按钮访问
    // ----------------------------------------------------------------

    public JButton getUndoButton() { return undoButton; }
    public JButton getCommitButton() { return commitButton; }
    public JButton getCancelButton() { return cancelButton; }
}