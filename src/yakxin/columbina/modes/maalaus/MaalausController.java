package yakxin.columbina.modes.maalaus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.ColumbinaSeqCommand;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.dto.outputs.ColumbinaOutputIntent;
import yakxin.columbina.data.curveSection.CurveSecUtils;
import yakxin.columbina.data.curveSection.LineExtendCurveSec;
import yakxin.columbina.utils.UtilsMath;


/**
 * Maalaus 绘制模式的控制器
 * <p>集中维护绘制状态、子模式、曲段列表和控制点列表，
 * 通过 {@link PropertyChangeSupport} 向 MaalausMapMode 和 InfoWindow 通知状态变更。
 */
public class MaalausController {
    private MaalausState state = MaalausState.INIT;  // 当前绘制状态
    private MaalausSubMode subMode = MaalausSubMode.LINE_EXTEND;  // 当前子模式（直线延伸 / 起点曲线延伸 / 交点曲线延伸）
    private ColumbinaEN startAnchor;  // 当前段的起点（= 上一段终点，或用户首次点击的起点）
    private ColumbinaEN startTangent;  // 当前段起点的切线方向
    private final List<AbstractCurveSec> secs = new ArrayList<>();  // 已完成曲段列表
    private final List<ColumbinaEN> pendingControlPoints = new ArrayList<>();  // 当前段已收集的控制点列表
    private ColumbinaEN previewPoint;  // 当前鼠标悬停位置对应的预览控制点
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);  // 属性变更通知支持

    // ----------------------------------------------------------------
    // PropertyChange 监听
    // ----------------------------------------------------------------

    /**
     * 注册属性变更监听器
     * @param listener 监听器
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * 移除属性变更监听器
     * @param listener 监听器
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    // ----------------------------------------------------------------
    // 状态变更
    // ----------------------------------------------------------------

    /**
     * 设置当前绘制状态（仅内部使用）
     */
    private void setState(MaalausState newState) {
        MaalausState oldState = this.state;
        this.state = newState;
        pcs.firePropertyChange("state", oldState, newState);
    }

    /**
     * 设置当前子模式
     * <p>切换子模式时应当清空当前段的未完成控制点，回到 DRAW 状态。
     * @param newSubMode 新子模式
     */
    private void setSubMode(MaalausSubMode newSubMode) {
        MaalausSubMode oldSubMode = this.subMode;
        this.subMode = newSubMode;
        pendingControlPoints.clear();
        previewPoint = null;
        pcs.firePropertyChange("subMode", oldSubMode, newSubMode);
        pcs.firePropertyChange("controlPoints", null, Collections.emptyList());
        pcs.firePropertyChange("previewPoint", null, null);
        setState(MaalausState.DRAW);
    }

    // ----------------------------------------------------------------
    // 起点
    // ----------------------------------------------------------------

    /**
     * 开始绘制：设置当前段起点并进入 DRAW 状态
     * @param anchor 起点坐标
     * @param tangent 起点切线方向
     */
    public void startDrawing(ColumbinaEN anchor, ColumbinaEN tangent) {
        this.startAnchor = anchor;
        this.startTangent = tangent;
        secs.clear();
        pendingControlPoints.clear();
        previewPoint = null;
        pcs.firePropertyChange("startAnchor", null, startAnchor);
        setState(MaalausState.DRAW);
    }

    // ----------------------------------------------------------------
    // 控制点操作
    // ----------------------------------------------------------------

    /**
     * 添加一个控制点
     * @param point 控制点坐标
     */
    public void addControlPoint(ColumbinaEN point) {
        pendingControlPoints.add(point);
        pcs.firePropertyChange("controlPoints", null, pendingControlPoints);
    }

    /**
     * 确认当前段并添加到曲段列表
     * <p>仅实现直线延伸模式（LINE_EXTEND）。
     */
    public void confirmSec() {
        if (pendingControlPoints.size() < subMode.getRequiredPointCount()) return;

        if (subMode == MaalausSubMode.LINE_EXTEND) {
            // 从当前待定控制点中找终点
            ColumbinaEN endPoint = pendingControlPoints.get(0);
            LineExtendCurveSec lineSec = new LineExtendCurveSec(startAnchor, startTangent, endPoint);
            secs.add(lineSec);
            startAnchor = lineSec.getEndEN();
            startTangent = new ColumbinaEN(
                Math.cos(lineSec.getEndAngleRad()),
                Math.sin(lineSec.getEndAngleRad())
            );
            // 清除当前待定控制点（其已被记录在lineSec，进而被记录在secs中）
            pendingControlPoints.clear();
            previewPoint = null;
            pcs.firePropertyChange("secs", null, secs);
            pcs.firePropertyChange("controlPoints", null, pendingControlPoints);
            pcs.firePropertyChange("previewPoint", null, null);
            pcs.firePropertyChange("startAnchor", null, startAnchor);
            setState(MaalausState.DRAW);
        }
    }

    /**
     * 设置预览控制点（鼠标移动时更新，不加入正式列表）
     * @param point 当前鼠标位置的坐标
     */
    public void setPreviewPoint(ColumbinaEN point) {
        this.previewPoint = point;
        pcs.firePropertyChange("previewPoint", null, point);
    }

    /**
     * 撤销上一个采样段
     * <p>移除采样段列表的最后一段，重新填充上一段的控制点至当前 pendingControlPoints，
     * 并回到该段的DRAW状态。
     */
    public void undoLastSec() {
        if (secs.isEmpty()) return;

        AbstractCurveSec lastSec = secs.remove(secs.size() - 1);
        // 清空待定控制点
        pendingControlPoints.clear();
        // 恢复上一段的起点
        startAnchor = lastSec.getStartEN();
        startTangent = new ColumbinaEN(
            Math.cos(lastSec.getStartAngleRad()),
            Math.sin(lastSec.getStartAngleRad())
        );
        previewPoint = pendingControlPoints.isEmpty() ? null : pendingControlPoints.get(pendingControlPoints.size() - 1);
        pcs.firePropertyChange("secs", null, secs);
        pcs.firePropertyChange("controlPoints", null, pendingControlPoints);
        pcs.firePropertyChange("previewPoint", null, previewPoint);
        pcs.firePropertyChange("startAnchor", null, startAnchor);
        setState(MaalausState.DRAW);
    }

    // ----------------------------------------------------------------
    // 提交与重置
    // ----------------------------------------------------------------

    /**
     * 提交全部曲段
     * <p>离散化所有曲段，通过意图系统转换为 JOSM Command 并提交到撤销重做栈。
     * @param interval 离散化采样间隔
     * @return 是否成功
     */
    public boolean commitAll(double interval) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null || secs.isEmpty()) return false;

        List<ColumbinaEN> allPoints = CurveSecUtils.sampleAll(secs, interval);
        if (allPoints.size() < 2) return false;

        List<Node> newNodes = new ArrayList<>();
        List<ColumbinaOutputIntent<?>> intents = new ArrayList<>();
        for (ColumbinaEN point : allPoints) {
            Node node = new Node(UtilsMath.toLatLon(point));
            newNodes.add(node);
            intents.add(new ColumbinaOutputIntent.AddThisNodeIfOK(node));
        }

        Way newWay = new Way();
        newWay.setNodes(newNodes);
        intents.add(new ColumbinaOutputIntent.AddThisWayIfOK(newWay));

        List<Command> commands = ColumbinaOutputIntent.toCommands(intents, ds);
        if (commands.isEmpty()) return false;

        UndoRedoHandler.getInstance().add(new ColumbinaSeqCommand(I18n.tr("Maalaus: Draw line"), commands));
        ds.setSelected(newWay);
        setState(MaalausState.DONE);
        return true;
    }

    /**
     * 取消绘制
     */
    public void abort() {
        setState(MaalausState.ABORT);
    }

    /**
     * 重置控制器到初始状态
     */
    public void reset() {
        state = MaalausState.INIT;
        subMode = MaalausSubMode.LINE_EXTEND;
        secs.clear();
        pendingControlPoints.clear();
        previewPoint = null;
        startAnchor = null;
        startTangent = null;
    }
    
    // ----------------------------------------------------------------
    // 暂停、继续
    // ----------------------------------------------------------------
    
    public void pauseDrawing() {
        setState(MaalausState.INFO);
    }
    
    public void continueDrawing() {
        if (secs.isEmpty() && startAnchor == null) setState(MaalausState.INIT);
        else setState(MaalausState.DRAW);
    }
    
    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public MaalausState getState() { return state; }

    public MaalausSubMode getSubMode() { return subMode; }

    public ColumbinaEN getStartAnchor() { return startAnchor; }

    public ColumbinaEN getStartTangent() { return startTangent; }

    public List<AbstractCurveSec> getSecs() { return secs; }

    public List<ColumbinaEN> getPendingControlPoints() { return pendingControlPoints; }

    public ColumbinaEN getPreviewPoint() { return previewPoint; }
}
