package yakxin.columbina.abstractClasses;

import java.util.List;

import yakxin.columbina.data.ColumbinaEN;

/**
 * 抽象基本曲段
 * <p>定义一条基本几何曲线（直线、圆曲线、缓和曲线）的统一接口。
 * 所有基本曲段都预留下述属性，并实现 walk（走行距离→坐标）和 sample（离散化）方法。
 */
public abstract class AbstractBasicCurveSec {

    /** 起点坐标 */
    protected ColumbinaEN startEN;
    /** 终点坐标 */
    protected ColumbinaEN endEN;
    /** 入曲线方向角（弧度，以东为 0，北为正） */
    protected double startAngleRad;
    /** 出曲线方向角（弧度，以东为 0，北为正） */
    protected double endAngleRad;
    /** 曲线长度（投影坐标系下，单位：米） */
    protected double length;
    /** 控制点列表（用于撤销时读取上一段的控制点） */
    protected List<ColumbinaEN> controlPointList;

    /**
     * 给定走行距离，获取从起点沿曲段走行对应距离后的坐标
     * @param distance 走行距离（投影坐标系下，单位：米，范围 [0, length]）
     * @return 对应位置的坐标
     */
    public abstract ColumbinaEN walk(double distance);

    /**
     * 离散化自身，返回等间距节点列表
     * @param interval 采样间距（投影坐标系下，单位：米）
     * @return 离散化后的节点列表（包含起点和终点）
     */
    public abstract List<ColumbinaEN> sample(double interval);

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public ColumbinaEN getStartEN() { return startEN; }
    public ColumbinaEN getEndEN() { return endEN; }
    public double getStartAngleRad() { return startAngleRad; }
    public double getEndAngleRad() { return endAngleRad; }
    public double getLength() { return length; }
    public List<ColumbinaEN> getControlPointList() { return controlPointList; }
}