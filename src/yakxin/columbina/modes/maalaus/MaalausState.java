package yakxin.columbina.modes.maalaus;

import org.openstreetmap.josm.tools.I18n;

/**
 * Maalaus 绘制模式的状态枚举
 */
public enum MaalausState {
    /** 初始状态，未开始绘制，等待用户点击起点 */
    INIT(I18n.tr("Click on the map to start drawing.")),
    /** 绘制状态，鼠标控制点跟随，实时预览 */
    DRAW(I18n.tr("Press Enter to finish drawing; press Backspace to undo the last section; press Space to pause mapping and interact with this panel; press Esc to cancel mapping.")),
    /** 暂停状态，鼠标不再跟随，可与信息窗口交互 */
    INFO(I18n.tr("Press Space to continue mapping.")),
    /** 完成状态 */
    DONE(""),
    /** 取消状态 */
    ABORT("");

    private final String statusInfo;

    MaalausState(String statusInfo) {
        this.statusInfo = statusInfo;
    }

    /**
     * 获取当前状态的提示信息
     * @return 提示文本
     */
    public String getStatusInfo() {
        return statusInfo;
    }
}