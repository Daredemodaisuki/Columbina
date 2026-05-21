package yakxin.columbina.modes.maalaus;

import org.openstreetmap.josm.tools.I18n;

/**
 * Maalaus 绘制模式的子模式枚举
 * <p>每个子模式定义了该模式下完成一段曲段所需的控制点数量。
 */
public enum MaalausSubMode {
    LINE_EXTEND(
            1,
            I18n.tr("Mouse click to determine the end point of the line section.")
    ),  // 直线延伸：需要1个控制点
    ARC_EXTEND(
            1,
            I18n.tr("Mouse click to determine the control point that determines the central angle of the curve.\n")
                    + I18n.tr("The turning direction (left/right or counter/clockwise) will be decided based on whether the control point lies to the left or right side of the current curve's extension line.")
    ),  // 起点曲线延伸：需要1个控制点
    PI_ARC_EXTEND(
            2,
            I18n.tr("Draw a curve section using the tangent baseline method. Based on the current curve sections' end point, ")
                    + I18n.tr("the tangent intersection (first control point), and the second control point, a circular arc tangent to both the incoming and outgoing directions is generated.\n")
                    + I18n.tr("First click to determine the intersection of the tangent intersection point for the tangent baseline method; ")
                    + I18n.tr("Second click to determine the 2nd control point, which decides the deflection angle of the curve section to be drawn.")
    );  // 交点曲线延伸：需要2个控制点（交点+方向点）

    private final int requiredPointCount;
    private final String info;

    MaalausSubMode(int requiredPointCount, String info) {
        this.requiredPointCount = requiredPointCount;
        this.info = info;
    }

    /**
     * 获取当前子模式所需的控制点数量
     * @return 控制点数量
     */
    public int getRequiredPointCount() {
        return requiredPointCount;
    }
    
    public String getInfo() {
        return info;
    }
}