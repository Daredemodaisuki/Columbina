package yakxin.columbina.modes.maalaus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.ColumbinaEN;

/**
 * Maalaus 绘制模式的会话数据模型
 * <p> Maalaus Model的纯数据部分
 * <p> 纯数据容器，集中维护绘制状态、子模式、曲段列表和控制点列表，
 * 通过 {@link PropertyChangeSupport} 向监听方（如 MaalausMapMode）通知状态变更。
 * <p> 所有数据变更方法均为包级私有，由 {@link MaalausDrawingService} 调用。
 */
public class MaalausSessionData {
    private MaalausState state = MaalausState.INIT;
    private MaalausSubMode subMode = MaalausSubMode.LINE_EXTEND;
    private ColumbinaEN startAnchor;
    private ColumbinaEN startTangent;
    private final List<AbstractCurveSec> secs = new ArrayList<>();
    private final List<ColumbinaEN> pendingControlPoints = new ArrayList<>();
    private ColumbinaEN previewPoint;
    private final PropertyChangeSupport propertyEventSource = new PropertyChangeSupport(this);

    // ----------------------------------------------------------------
    // PropertyChange 监听
    // ----------------------------------------------------------------

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyEventSource.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyEventSource.removePropertyChangeListener(listener);
    }

    // ----------------------------------------------------------------
    // 数据变更（包级私有，由 MaalausDrawingService 调用）
    // ----------------------------------------------------------------

    void setState(MaalausState newState) {
        MaalausState oldState = this.state;
        this.state = newState;
        propertyEventSource.firePropertyChange("state", oldState, newState);
    }

    void setSubMode(MaalausSubMode newSubMode) {
        MaalausSubMode oldSubMode = this.subMode;
        this.subMode = newSubMode;
        propertyEventSource.firePropertyChange("subMode", oldSubMode, newSubMode);
    }

    void setStartAnchor(ColumbinaEN anchor) {
        ColumbinaEN old = this.startAnchor;
        this.startAnchor = anchor;
        propertyEventSource.firePropertyChange("startAnchor", old, anchor);
    }

    void setStartTangent(ColumbinaEN tangent) {
        this.startTangent = tangent;
    }

    void setPreviewPoint(ColumbinaEN point) {
        ColumbinaEN old = this.previewPoint;
        this.previewPoint = point;
        propertyEventSource.firePropertyChange("previewPoint", old, point);
    }

    void addSec(AbstractCurveSec sec) {
        secs.add(sec);
        propertyEventSource.firePropertyChange("secs", null, secs);
    }

    void removeLastSec() {
        if (!secs.isEmpty()) {
            secs.remove(secs.size() - 1);
            propertyEventSource.firePropertyChange("secs", null, secs);
        }
    }

    void clearPendingControlPoints() {
        pendingControlPoints.clear();
        propertyEventSource.firePropertyChange("controlPoints", null, pendingControlPoints);
    }

    void addControlPoint(ColumbinaEN point) {
        pendingControlPoints.add(point);
        propertyEventSource.firePropertyChange("controlPoints", null, pendingControlPoints);
    }

    void updateLastControlPoint(ColumbinaEN point) {
        if (pendingControlPoints.isEmpty()) return;
        pendingControlPoints.set(pendingControlPoints.size() - 1, point);
        propertyEventSource.firePropertyChange("controlPoints", null, pendingControlPoints);
    }

    // ----------------------------------------------------------------
    // 重置（静默，不触发事件）
    // ----------------------------------------------------------------

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