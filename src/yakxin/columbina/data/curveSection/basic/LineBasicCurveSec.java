package yakxin.columbina.data.curveSection.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import yakxin.columbina.abstractClasses.AbstractBasicCurveSec;
import yakxin.columbina.data.ColumbinaEN;

/**
 * 直线基本曲段
 * <p>由起点和终点定义的一条直线段。
 * <p>walk 通过线性插值实现，sample 按间距等分线段生成离散节点。
 */
public class LineBasicCurveSec extends AbstractBasicCurveSec {

    /**
     * 构造函数
     * @param start 起点坐标
     * @param end 终点坐标
     */
    public LineBasicCurveSec(ColumbinaEN start, ColumbinaEN end) {
        this.startEN = start;
        this.endEN = end;
        this.length = start.distance(end);

        if (length > 0) {
            this.startAngleRad = new ColumbinaEN(start, end).bearingRad();
            this.endAngleRad = this.startAngleRad;
        } else {
            this.startAngleRad = 0;
            this.endAngleRad = 0;
        }

        this.controlPointList = Collections.singletonList(new ColumbinaEN(end));
    }

    // ----------------------------------------------------------------
    // walk / sample
    // ----------------------------------------------------------------

    @Override
    public ColumbinaEN walk(double distance) {
        if (length <= 0) return startEN;
        double t = Math.max(0, Math.min(1, distance / length));
        return new ColumbinaEN(startEN.interpolate(endEN, t));
    }

    @Override
    public List<ColumbinaEN> sample(double interval) {
        List<ColumbinaEN> points = new ArrayList<>();
        if (length <= 0) {
            points.add(startEN);
            return points;
        }

        int segments = Math.max(1, (int) Math.ceil(length / Math.max(1.0, interval)));
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            points.add(new ColumbinaEN(startEN.interpolate(endEN, t)));
        }
        return points;
    }
}