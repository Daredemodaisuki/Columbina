package yakxin.columbina.modes.maalaus;

import java.util.List;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.columbinaCurveSection.LineExtendCurveSec;
import yakxin.columbina.data.dto.modelsDTO.maalaus.LineExtendDisplayData;
import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;
import yakxin.columbina.modes.maalaus.secInfoPanel.LineExtendSecInfoPanel;
import yakxin.columbina.modes.maalaus.secInfoPanel.SecInfoPanel;

/**
 * Maalaus 绘制模式的子模式枚举
 * <p> 其中extractDisplayData()系Maalaus Model->View中间的数据转换器，所以该文件需要引用MaalausSessionData，但不持有数据，
 *     由Controller（MaalausMapMode）委托View调用（MaalausInfoWindow.updateSecInfoValues()）
 * <p> 每个子模式定义了该模式下完成一段曲段所需的控制点数量。
 */
public enum MaalausSubMode {
    // 直线延伸：需要1个控制点
    LINE_EXTEND(
            1,
            I18n.tr("Mouse click to determine the end point of the line section.")
    ) {
        @Override
        public SecInfoPanel createSecInfoPanel() {
            return new LineExtendSecInfoPanel();
        }

        @Override
        public AbstractCurveSec createCurveSec(ColumbinaEN startAnchor, ColumbinaEN startTangent, List<ColumbinaEN> controlPoints) {
            return new LineExtendCurveSec(startAnchor, startTangent, controlPoints.get(0));
        }

        @Override
        public SecDisplayData extractDisplayData(MaalausSessionData session) {
            ColumbinaEN start = session.getStartAnchor();
            if (start == null) return null;

            ColumbinaEN end = !session.getPendingControlPoints().isEmpty()
                    ? session.getPendingControlPoints().get(session.getPendingControlPoints().size() - 1)
                    : session.getPreviewPoint();
            return LineExtendCurveSec.calculateDisplayData(start, end);
        }

        @Override
        public List<ColumbinaEN> generateAllControlPoints(MaalausSessionData session, SecDisplayData data) {
            if (!(data instanceof LineExtendDisplayData)) return List.of();
            ColumbinaEN start = session.getStartAnchor();
            if (start == null) return List.of();
            LineExtendDisplayData led = (LineExtendDisplayData) data;
            ColumbinaEN cp = LineExtendCurveSec.calculateControlPoint(start, led.bearingDeg, led.lengthM);
            if (cp == null) return List.of();
            return List.of(cp);
        }
    },
    // 起点曲线延伸：需要1个控制点
    ARC_EXTEND(
            1,
            I18n.tr("Mouse click to determine the control point that determines the central angle of the curve.\n")
                    + I18n.tr("The turning direction (left/right or counter/clockwise) will be decided based on whether the control point lies to the left or right side of the current curve's extension line.")
    ) {
        @Override
        public SecInfoPanel createSecInfoPanel() {
            return null;
        }

        @Override
        public AbstractCurveSec createCurveSec(ColumbinaEN startAnchor, ColumbinaEN startTangent, List<ColumbinaEN> controlPoints) {
            return null;
        }

        @Override
        public SecDisplayData extractDisplayData(MaalausSessionData session) {
            return null;
        }

        @Override
        public List<ColumbinaEN> generateAllControlPoints(MaalausSessionData session, SecDisplayData data) {
            return List.of();
        }
    },
    // 交点曲线延伸：需要2个控制点（交点+方向点）
    PI_ARC_EXTEND(
            2,
            I18n.tr("Draw a curve section using the tangent baseline method. Based on the current curve sections' end point, ")
                    + I18n.tr("the tangent intersection (first control point), and the second control point, a circular arc tangent to both the incoming and outgoing directions is generated.\n")
                    + I18n.tr("First click to determine the intersection of the tangent intersection point for the tangent baseline method; ")
                    + I18n.tr("Second click to determine the 2nd control point, which decides the deflection angle of the curve section to be drawn.")
    ) {
        @Override
        public SecInfoPanel createSecInfoPanel() {
            return null;
        }

        @Override
        public AbstractCurveSec createCurveSec(ColumbinaEN startAnchor, ColumbinaEN startTangent, List<ColumbinaEN> controlPoints) {
            return null;
        }

        @Override
        public SecDisplayData extractDisplayData(MaalausSessionData session) {
            return null;
        }

        @Override
        public List<ColumbinaEN> generateAllControlPoints(MaalausSessionData session, SecDisplayData data) {
            return List.of();
        }
    };

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

    // ============================================================
    // 曲段信息面板 — 由各子模式常量自行定义
    // ============================================================

    /**
     * 创建当前子模式的曲段信息面板
     * <p>各枚举常量可覆写此方法以提供自定义面板。
     * @return 面板实例，返回 {@code null} 表示无曲段信息需显示
     */
    abstract public SecInfoPanel createSecInfoPanel();

    /**
     * 根据当前子模式创建曲段
     * <p>各枚举常量将起点、切线和控制点列表转化为对应的曲段实例。
     * @param startAnchor 当前段起点（上一段终点，或首次点击的起点）
     * @param startTangent 当前段起点切线方向
     * @param controlPoints 控制点列表（已确认）
     * @return 曲段实例
     */
    abstract public AbstractCurveSec createCurveSec(ColumbinaEN startAnchor, ColumbinaEN startTangent, List<ColumbinaEN> controlPoints);

    /**
     * 从当前 session 提取子模式特有的显示数据
     * <p>各枚举常量将 session 中的原始数据转换为不可变的 {@link SecDisplayData} 对象，
     * 供信息面板的 {@link SecInfoPanel#updateValues(SecDisplayData)} 消费。
     * @param session 当前会话数据
     * @return 显示数据对象，返回 {@code null} 表示无数据可显示
     */
    abstract public SecDisplayData extractDisplayData(MaalausSessionData session);

    /**
     * 根据完整的显示数据生成该子模式所需的全部控制点
     * <p>INFO 状态下用户点击「添加曲段」时调用，清空当前待提交列表后依次添加返回的每个点。
     * 在 INFO 状态下编辑输入框时，取列表中的最后一个点作为预览点。
     * @param session 当前会话数据
     * @param data 显示数据（由输入框解析而来）
     * @return 控制点列表（空列表表示无法生成）
     */
    abstract public List<ColumbinaEN> generateAllControlPoints(MaalausSessionData session, SecDisplayData data);
}