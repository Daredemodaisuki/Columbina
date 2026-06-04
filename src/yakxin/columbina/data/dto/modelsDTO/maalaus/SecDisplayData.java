package yakxin.columbina.data.dto.modelsDTO.maalaus;

import yakxin.columbina.modes.maalaus.MaalausSubMode;
import yakxin.columbina.modes.maalaus.secInfoPanel.SecInfoPanel;

/**
 * 曲段信息面板的显示数据标记接口
 * <p>各子模式通过 {@link MaalausSubMode#extractDisplayData}
 * 将当前段的起点和控制点列表提取为不可变的显示数据对象，传递给
 * {@link SecInfoPanel#updateValues(SecDisplayData)}。
 * <p>具体子类携带该子模式特有的显示字段（如直线延伸的方位角、长度；
 * 圆曲线的半径、转角等）。
 */
public interface SecDisplayData {
}