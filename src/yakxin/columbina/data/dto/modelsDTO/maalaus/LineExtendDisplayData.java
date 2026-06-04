package yakxin.columbina.data.dto.modelsDTO.maalaus;

import yakxin.columbina.modes.maalaus.MaalausSubMode;

/**
 * 直线延伸子模式的显示数据
 * <p>由 {@link MaalausSubMode#extractDisplayData} 创建，
 * 包含当前待确认直线段的方位角和长度。
 */
public class LineExtendDisplayData implements SecDisplayData {
    public final double bearingDeg;
    public final double lengthM;

    public LineExtendDisplayData(double bearingDeg, double lengthM) {
        this.bearingDeg = bearingDeg;
        this.lengthM = lengthM;
    }
}