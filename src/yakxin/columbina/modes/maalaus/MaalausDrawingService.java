package yakxin.columbina.modes.maalaus;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.ColumbinaSeqCommand;
import yakxin.columbina.data.dto.featuresDTO.outputs.ColumbinaOutputIntent;
import yakxin.columbina.utils.UtilsMath;

/**
 * Maalaus 绘制模式的业务逻辑服务
 * <p> Maalaus Model的业务逻辑的部分
 * <p> 封装所有与绘制相关的业务操作，操作 {@link MaalausSessionData} 并触发状态变更通知。
 * MaalausMapMode 作为 Controller 协调键鼠事件对该服务的调用。
 */
public class MaalausDrawingService {
    private final MaalausSessionData session;

    public MaalausDrawingService(MaalausSessionData session) {
        this.session = session;
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
        session.setStartAnchor(anchor);
        session.setStartTangent(tangent);
        session.getSecs().clear();
        session.getPendingControlPoints().clear();
        session.setPreviewPoint(null);
        session.setState(MaalausState.DRAW);
    }

    // ----------------------------------------------------------------
    // 控制点操作
    // ----------------------------------------------------------------

    /**
     * 添加一个控制点
     * @param point 控制点坐标
     */
    public void addControlPoint(ColumbinaEN point) {
        session.addControlPoint(point);
    }

    /**
     * 清空当前待提交控制点列表
     */
    public void clearPendingControlPoints() {
        session.clearPendingControlPoints();
    }

    /**
     * 确认当前段并添加到曲段列表
     * <p>通过 {@link MaalausSubMode#createCurveSec} 工厂方法创建曲段，
     * 新增子模式时无需修改此方法。
     */
    public void confirmSec() {
        if (session.getPendingControlPoints().size() < session.getSubMode().getRequiredPointCount()) return;

        AbstractCurveSec sec = session.getSubMode().createCurveSec(
                session.getStartAnchor(),
                session.getStartTangent(),
                session.getPendingControlPoints()
        );
        if (sec == null) return;

        // 更新起点和起点方向
        session.addSec(sec);
        session.setStartAnchor(sec.getEndEN());
        session.setStartTangent(new ColumbinaEN(
                Math.cos(sec.getEndAngleRad()),
                Math.sin(sec.getEndAngleRad())
        ));
        session.clearPendingControlPoints();
        session.setPreviewPoint(null);
        // session.setState(MaalausState.DRAW);  // 不强制状态切换
    }

    /**
     * 设置预览控制点（鼠标移动时更新，不加入正式列表）
     * @param point 当前鼠标位置的坐标
     */
    public void setPreviewPoint(ColumbinaEN point) {
        session.setPreviewPoint(point);
    }

    /**
     * 更新最后一个待提交控制点的坐标
     * <p>在 INFO 状态下用户编辑输入框时调用，根据反算的控制点更新预览。
     * @param point 新的控制点坐标
     */
    public void updateLastControlPoint(ColumbinaEN point) {
        // TODO：可能弃用
        session.updateLastControlPoint(point);
    }

    /**
     * 撤销上一个采样段
     * <p>移除采样段列表的最后一段，重新填充上一段的控制点至当前 pendingControlPoints，
     * 并回到该段的 DRAW 状态。
     */
    public void undoLastSec() {
        List<AbstractCurveSec> secs = session.getSecs();
        if (secs.isEmpty()) return;

        AbstractCurveSec lastSec = secs.get(secs.size() - 1);
        session.removeLastSec();
        session.clearPendingControlPoints();
        session.setStartAnchor(lastSec.getStartEN());
        session.setStartTangent(new ColumbinaEN(
            Math.cos(lastSec.getStartAngleRad()),
            Math.sin(lastSec.getStartAngleRad())
        ));
        session.setPreviewPoint(null);
        session.setState(MaalausState.DRAW);
    }

    // ----------------------------------------------------------------
    // 提交与取消
    // ----------------------------------------------------------------

    /**
     * 提交全部曲段
     * <p>离散化所有曲段，通过意图系统转换为 JOSM Command 并提交到撤销重做栈。
     * @param interval 离散化采样间隔
     * @return 是否成功
     */
    public boolean commitAll(double interval) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null || session.getSecs().isEmpty()) return false;

        List<ColumbinaEN> allPoints = AbstractCurveSec.sampleAll(session.getSecs(), interval);
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
        session.setState(MaalausState.DONE);
        return true;
    }

    /**
     * 取消绘制
     */
    public void abort() {
        session.setState(MaalausState.ABORT);
    }

    // ----------------------------------------------------------------
    // 暂停、继续
    // ----------------------------------------------------------------

    public void pauseDrawing() {
        session.setState(MaalausState.INFO);
    }

    public void continueDrawing() {
        if (session.getSecs().isEmpty() && session.getStartAnchor() == null)
            session.setState(MaalausState.INIT);
        else
            session.setState(MaalausState.DRAW);
    }
}