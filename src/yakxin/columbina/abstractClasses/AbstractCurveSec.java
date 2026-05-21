package yakxin.columbina.abstractClasses;

import java.util.ArrayList;
import java.util.List;

import yakxin.columbina.data.ColumbinaEN;

/**
 * 抽象组合曲段
 * <p>由多个基本曲段组成（如交点曲线延伸可能对应：直线 + 缓圆缓）。
 * <p>重写了 walk 和 sample，通过遍历 basicCurveSecList 将调用委托给各基本曲段：
 * <ul>
 *   <li>walk：从第一段基本曲段开始减除长度，找到目标距离所在段后委托调用</li>
 *   <li>sample：依次离散化各基本曲段，拼接节点列表（自动去除段间重复点）</li>
 * </ul>
 * <p>具体子类在构造函数中填充 basicCurveSecList，然后调用 initFromBasicSecs() 同步属性。
 */
public abstract class AbstractCurveSec extends AbstractBasicCurveSec {

    /** 基本曲段列表（按顺序排列，首尾相连） */
    protected List<AbstractBasicCurveSec> basicCurveSecList;

    /**
     * 从 basicCurveSecList 同步派生属性（startEN、endEN、startAngleRad 等）
     * <p>具体子类在构造函数末尾必须调用此方法。
     */
    protected void initFromBasicSecs() {
        if (basicCurveSecList == null || basicCurveSecList.isEmpty()) return;

        AbstractBasicCurveSec first = basicCurveSecList.get(0);
        AbstractBasicCurveSec last = basicCurveSecList.get(basicCurveSecList.size() - 1);

        this.startEN = first.startEN;
        this.endEN = last.endEN;
        this.startAngleRad = first.startAngleRad;
        this.endAngleRad = last.endAngleRad;

        this.length = 0;
        for (AbstractBasicCurveSec sec : basicCurveSecList) {
            this.length += sec.length;
        }
    }

    // ----------------------------------------------------------------
    // walk / sample（委托给 basicCurveSecList）
    // ----------------------------------------------------------------

    @Override
    public ColumbinaEN walk(double distance) {
        if (basicCurveSecList == null || basicCurveSecList.isEmpty()) return null;
        if (distance <= 0) return startEN;

        double remaining = distance;
        for (AbstractBasicCurveSec sec : basicCurveSecList) {
            if (remaining <= sec.length) {
                return sec.walk(remaining);
            }
            remaining -= sec.length;
        }
        return endEN;
    }

    @Override
    public List<ColumbinaEN> sample(double interval) {
        List<ColumbinaEN> allPoints = new ArrayList<>();
        if (basicCurveSecList == null || basicCurveSecList.isEmpty()) return allPoints;

        for (int i = 0; i < basicCurveSecList.size(); i++) {
            AbstractBasicCurveSec sec = basicCurveSecList.get(i);
            List<ColumbinaEN> secPoints = sec.sample(interval);

            for (int j = 0; j < secPoints.size(); j++) {
                if (i > 0 && j == 0) continue;
                allPoints.add(secPoints.get(j));
            }
        }
        return allPoints;
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public List<AbstractBasicCurveSec> getBasicCurveSecList() { return basicCurveSecList; }
}