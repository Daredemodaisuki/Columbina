package yakxin.columbina.modes.maalaus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.columbinaCurveSection.LineExtendCurveSec;
import yakxin.columbina.data.dto.modelsDTO.maalaus.LineExtendDisplayData;
import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;
import yakxin.columbina.modes.maalaus.secInfoPanel.LineExtendSecInfoPanel;
import yakxin.columbina.modes.maalaus.secInfoPanel.SecInfoPanel;
import yakxin.columbina.utils.utilsView.Previewer;

/**
 * Maalaus 绘制模式的子模式枚举
 * <p> extractDisplayData()系Maalaus Model->View中间的数据转换器，
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
        public SecDisplayData extractDisplayData(ColumbinaEN startAnchor, List<ColumbinaEN> controlPoints) {
            if (startAnchor == null) return null;
            ColumbinaEN end = !controlPoints.isEmpty()
                    ? controlPoints.get(controlPoints.size() - 1)
                    : null;
            if (end == null) return null;
            return LineExtendCurveSec.calculateDisplayData(startAnchor, end);
        }

        @Override
        public List<ColumbinaEN> generateAllControlPoints(ColumbinaEN startAnchor, SecDisplayData data) {
            if (!(data instanceof LineExtendDisplayData)) return List.of();
            if (startAnchor == null) return List.of();
            LineExtendDisplayData led = (LineExtendDisplayData) data;
            ColumbinaEN cp = LineExtendCurveSec.calculateControlPoint(startAnchor, led.bearingDeg, led.lengthM);
            if (cp == null) return List.of();
            return List.of(cp);
        }

        @Override
        public ColumbinaEN calculatePendingControlPoint(ColumbinaEN startAnchor, ColumbinaEN startTangent,
                                                        ColumbinaEN mousePos, List<ColumbinaEN> currentPendingCPs) {
            return mousePos;  // LINE: 恒等映射，鼠标位置即为控制点
        }

        @Override
        public List<Previewer.Renderable> calculatePreviewGeometry(ColumbinaEN startAnchor, ColumbinaEN startTangent,
                                                                    List<ColumbinaEN> pendingCPs, ColumbinaEN previewCP) {
            // LINE: 返回直线预览线（蓝色半透明），起点到预览控制点之间的等分线段
            if (startAnchor == null || previewCP == null) return List.of();
            double dist = startAnchor.distance(previewCP);
            if (dist <= 0) return List.of();
            int segments = Math.max(1, (int) Math.ceil(dist / 5.0));
            List<ColumbinaEN> pts = new ArrayList<>(segments + 1);
            for (int i = 0; i <= segments; i++) {
                double t = (double) i / segments;
                pts.add(new ColumbinaEN(startAnchor.interpolate(previewCP, t)));
            }
            return List.of(new Previewer.RenderableLine(pts, new Color(0, 120, 255, 180), 3f, false));
        }
    },
    // 起点曲线延伸：需要2个控制点
    ARC_EXTEND(
            2,
            // TODO：按照新需求修改描述
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
        public SecDisplayData extractDisplayData(ColumbinaEN startAnchor, List<ColumbinaEN> controlPoints) {
            return null;
        }

        @Override
        public List<ColumbinaEN> generateAllControlPoints(ColumbinaEN startAnchor, SecDisplayData data) {
            return List.of();
        }

        @Override
        public ColumbinaEN calculatePendingControlPoint(ColumbinaEN startAnchor, ColumbinaEN startTangent,
                                                        ColumbinaEN mousePos, List<ColumbinaEN> currentPendingCPs) {
            return mousePos;  // TODO: 实现 ARC_EXTEND 的几何变换（法线投影/圆弧交点）
        }

        @Override
        public List<Previewer.Renderable> calculatePreviewGeometry(ColumbinaEN startAnchor, ColumbinaEN startTangent,
                                                                    List<ColumbinaEN> pendingCPs, ColumbinaEN previewCP) {
            return List.of();  // TODO: 实现 ARC_EXTEND 的辅助几何（法线/圆心/转角线/弧）
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
        public SecDisplayData extractDisplayData(ColumbinaEN startAnchor, List<ColumbinaEN> controlPoints) {
            return null;
        }

        @Override
        public List<ColumbinaEN> generateAllControlPoints(ColumbinaEN startAnchor, SecDisplayData data) {
            return List.of();
        }

        @Override
        public ColumbinaEN calculatePendingControlPoint(ColumbinaEN startAnchor, ColumbinaEN startTangent,
                                                        ColumbinaEN mousePos, List<ColumbinaEN> currentPendingCPs) {
            return mousePos;  // TODO: 实现 PI_ARC_EXTEND 的几何变换
        }

        @Override
        public List<Previewer.Renderable> calculatePreviewGeometry(ColumbinaEN startAnchor, ColumbinaEN startTangent,
                                                                    List<ColumbinaEN> pendingCPs, ColumbinaEN previewCP) {
            return List.of();  // TODO: 实现 PI_ARC_EXTEND 的辅助几何
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
     * 根据起点和控制点列表提取子模式特有的显示数据
     * <p>各枚举常量将起点和控制点列表转换为不可变的 {@link SecDisplayData} 对象，
     * 供信息面板的 {@link SecInfoPanel#updateValues(SecDisplayData)} 消费。
     * @param startAnchor          当前段起点
     * @param controlPoints 当前待提交控制点列表（子模式按需取用）
     * @return 显示数据对象，返回 {@code null} 表示无数据可显示
     */
    abstract public SecDisplayData extractDisplayData(ColumbinaEN startAnchor, List<ColumbinaEN> controlPoints);

    /**
     * 根据完整的显示数据生成该子模式所需的全部控制点
     * <p>INFO 状态下用户点击「添加曲段」时调用，清空当前待提交列表后依次添加返回的每个点。
     * 在 INFO 状态下编辑输入框时，取列表中的最后一个点作为预览点。
     * @param startAnchor 当前段起点
     * @param data 显示数据（由输入框解析而来）
     * @return 控制点列表（空列表表示无法生成）
     */
    abstract public List<ColumbinaEN> generateAllControlPoints(ColumbinaEN startAnchor, SecDisplayData data);

    /**
     * 根据鼠标位置和当前状态计算派生控制点
     * <p>子模式将鼠标原始坐标映射为实际控制点（如法线投影、圆弧交点等）。
     * <p>DRAW 状态下鼠标移动和点击时由 Controller 调用，结果用于设置预览点和添加控制点。
     * @param startAnchor 当前段起点
     * @param startTangent 当前段起点切线方向
     * @param mousePos 鼠标原始坐标
     * @param currentPendingCPs 当前已确定的待提交控制点列表
     * @return 派生控制点坐标
     */
    abstract public ColumbinaEN calculatePendingControlPoint(
            ColumbinaEN startAnchor,
            ColumbinaEN startTangent,
            ColumbinaEN mousePos,
            List<ColumbinaEN> currentPendingCPs
    );

    /**
     * 计算当前阶段的辅助几何渲染原语
     * <p>子模式返回辅助几何对象列表（法线、圆心、转角线、弧预览等），
     * 由 {@link Previewer#setRenderables(List)} 统一点绘。
     * @param startAnchor 当前段起点
     * @param startTangent 当前段起点切线方向
     * @param pendingCPs 当前已确定的待提交控制点列表
     * @param previewCP 当前预览控制点（派生后）
     * @return 辅助几何渲染原语列表
     */
    abstract public List<Previewer.Renderable> calculatePreviewGeometry(
            ColumbinaEN startAnchor,
            ColumbinaEN startTangent,
            List<ColumbinaEN> pendingCPs,
            ColumbinaEN previewCP
    );
}