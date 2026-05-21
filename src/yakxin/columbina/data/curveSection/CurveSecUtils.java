package yakxin.columbina.data.curveSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.utils.UtilsMath;

/**
 * 曲段工具
 * <p>负责拼接多段曲段的离散结果，并检查首尾是否连续。
 */
public final class CurveSecUtils {

    private CurveSecUtils() {
    }

    /**
     * 离散化全部曲段并拼接点列。
     * @param secs 曲段列表
     * @param interval 采样间隔
     * @return 拼接后的离散点列
     */
    public static List<ColumbinaEN> sampleAll(List<? extends AbstractCurveSec> secs, double interval) {
        if (secs == null || secs.isEmpty()) return Collections.emptyList();

        List<ColumbinaEN> allPoints = new ArrayList<>();
        AbstractCurveSec previous = null;
        for (AbstractCurveSec sec : secs) {
            if (sec == null) continue;

            if (previous != null && previous.getEndEN().distance(sec.getStartEN()) > UtilsMath.EPSILON_EASING) {
                throw new IllegalArgumentException("Curve sections are not connected.");
            }

            List<ColumbinaEN> secPoints = sec.sample(interval);
            for (int i = 0; i < secPoints.size(); i++) {
                if (!allPoints.isEmpty() && i == 0) continue;
                allPoints.add(secPoints.get(i));
            }
            previous = sec;
        }
        return allPoints;
    }
}