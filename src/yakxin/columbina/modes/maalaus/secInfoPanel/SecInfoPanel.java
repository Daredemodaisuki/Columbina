package yakxin.columbina.modes.maalaus.secInfoPanel;

import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;
import yakxin.columbina.modes.maalaus.MaalausSubMode;

import javax.swing.*;

/**
 * 曲段信息面板接口
 * <p>各子模式通过 {@link MaalausSubMode#createSecInfoPanel()} 返回自己的面板实例，
 * 信息窗口仅做托管容器。面板自持控件与布局，通过
 * {@link #updateValues(SecDisplayData)} 接收数据刷新显示，
 * 未来可在 {@link #setEditable(boolean)} 中切换标签/输入框模式。
 */
public interface SecInfoPanel {
    /** 输入变更监听器，当用户在输入框中修改数值时触发 */
    interface InputChangeListener {
        void onInputChanged(SecDisplayData data);
    }

    /** 返回面板组件，供信息窗口放入布局 */
    JPanel getPanel();
    /** 接收显示数据并刷新面板，每次鼠标移动/状态变更时调用 */
    void updateValues(SecDisplayData data);
    /** 切换编辑模式：DRAW 时为只读标签，INFO 时可改造为输入框 */
    void setEditable(boolean editable);
    /** 注册输入变更监听器 */
    void setInputChangeListener(InputChangeListener listener);
    /** 请求焦点到第一个输入框（进入 INFO 状态时调用） */
    void requestFieldFocus();
}
