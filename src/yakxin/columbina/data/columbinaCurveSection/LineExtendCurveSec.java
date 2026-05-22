package yakxin.columbina.data.columbinaCurveSection;

import java.util.Collections;

import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.columbinaCurveSection.basic.LineBasicCurveSec;
import yakxin.columbina.data.dto.modelsDTO.maalaus.LineExtendDisplayData;

/**
 * 直线延伸曲段
 * <p>对应子模式 LINE_EXTEND，以当前曲段前头端点为起点，控制点为终点产生简单直线。
 * <p>内部仅包含一个 LineBasicCurveSec。
 */
public class LineExtendCurveSec extends AbstractCurveSec {

    /**
     * 构造函数
     * @param startAnchor 当前段的起点（= 上一段终点，或用户首次点击的起点）
     * @param startTangent 当前段起点切线方向
     * @param controlPoint 控制点（终点）
     */
    public LineExtendCurveSec(ColumbinaEN startAnchor, ColumbinaEN startTangent, ColumbinaEN controlPoint) {
        LineBasicCurveSec lineSec = new LineBasicCurveSec(startAnchor, controlPoint);
        this.basicCurveSecList = Collections.singletonList(lineSec);

        initFromBasicSecs();

        this.controlPointList = Collections.singletonList(controlPoint);
    }

    /**
     * 根据起点和终点计算显示数据
     * @param start 起点坐标
     * @param end 终点坐标
     * @return 显示数据，任一参数为 null 时返回 null
     */
    public static LineExtendDisplayData calculateDisplayData(ColumbinaEN start, ColumbinaEN end) {
        if (start == null || end == null) return null;

        ColumbinaEN vec = new ColumbinaEN(start, end);
        double dist = start.distance(end);
        double bearingDeg = Math.toDegrees(vec.bearingRad());
        bearingDeg = (bearingDeg % 360 + 360) % 360;
        return new LineExtendDisplayData(bearingDeg, dist);
    }
}