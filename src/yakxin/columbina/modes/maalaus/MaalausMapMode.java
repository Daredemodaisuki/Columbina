package yakxin.columbina.modes.maalaus;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;

import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.abstractClasses.AbstractCurveSec;
import yakxin.columbina.data.dto.modelsDTO.maalaus.LineExtendDisplayData;
import yakxin.columbina.data.dto.modelsDTO.maalaus.SecDisplayData;
import yakxin.columbina.utils.UtilsUI;
import yakxin.columbina.utils.utilsView.Previewer;

/**
 * Maalaus 绘制模式的 MapMode
 * <p> Maalaus Controller
 * <p> 处理鼠标和键盘事件，协调 {@link MaalausSessionData}、{@link MaalausDrawingService}、
 * {@link Previewer} 和 {@link MaalausInfoWindow} 之间的交互。
 * <p> 当前仅实现直线延伸（LINE_EXTEND）模式。
 */
public class MaalausMapMode extends MapMode implements MaalausInfoWindow.UserEventListener {

    private final MaalausSessionData session;
    private final MaalausDrawingService service;
    private final Previewer previewer;
    private final KeyEventDispatcher keyDispatcher;
    private final java.beans.PropertyChangeListener sessionListener;
    private MaalausInfoWindow infoWindow;
    private SecDisplayData lastDisplayData;  // 用户最近一次在 INFO 状态下输入的完整参数

    /**
     * 构造函数
     */
    public MaalausMapMode() {
        super(
            "Maalaus",
            "maalaus",
            "Maalaus drawing mode",
            Shortcut.registerShortcut("maalaus:maalaus", "Maalaus", KeyEvent.VK_M, Shortcut.DIRECT),
            Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        );
        this.session = new MaalausSessionData();
        this.service = new MaalausDrawingService(session);
        this.previewer = new Previewer();
        this.keyDispatcher = new MaalausKeyDispatcher();

        // 定义统一的监听器（注册到MaalausSessionData的PropertyChangeSupport后，会话状态变更时触发，于进入模式时注册，退出模式时注销）
        this.sessionListener = (PropertyChangeEvent event) -> {
            // 如果完成或取消则退出模式
            if ("state".equals(event.getPropertyName())) {
                MaalausState newState = (MaalausState) event.getNewValue();
                if (newState == MaalausState.DONE || newState == MaalausState.ABORT) {
                    leaveMode();
                    return;
                }
                // 进入INFO时缓存当前显示数据，确保不修改输入框直接点击「添加曲段」时lastDisplayData不为null
                if (newState == MaalausState.INFO) {
                    // 参数通过已确定的待提交控制点+进入INFO前在DRAW下最后一个跟随鼠标滑动的待提交控制点决定
                    // TODO：把「合并已确定的待提交控制点+最后一个跟随鼠标滑动的待提交控制点」提取为方法「取临预览待提交控制点」
                    List<ColumbinaEN> pendingControlPointsForUpdate = new ArrayList<>(session.getPendingControlPoints());
                    pendingControlPointsForUpdate.add(session.getPreviewPoint());
                    lastDisplayData = session.getSubMode().extractDisplayData(session.getStartAnchor(), pendingControlPointsForUpdate);
                }
            }
            // 更新信息面板内容
            if (infoWindow != null) {
                infoWindow.updateMode(session.getSubMode().name());
                infoWindow.updateStatus(session.getState().name(), session.getState().getStatusInfo());
                infoWindow.updateSecCount(session.getSecs().size());
                infoWindow.updateInfo(session.getSubMode().getInfo());
                // 判断状态切换输入框可编辑性
                infoWindow.setSecInfoEditable(session.getState() == MaalausState.INFO);
            }
            refreshPreview();
            if (MainApplication.getMap() != null && MainApplication.getMap().mapView != null) {
                MainApplication.getMap().mapView.repaint();
            }
        };
    }

    // ----------------------------------------------------------------
    // 模式生命周期
    // ----------------------------------------------------------------

    @Override
    public void enterMode() {
        super.enterMode();
        MapView mv = MainApplication.getMap().mapView;

        // 显式重置会话状态，防止上次退出时可能存在的状态污染
        session.reset();

        // 注册预览绘制器
        mv.addTemporaryLayer(previewer);

        // MapMode不会自动接收MapView的鼠标事件，需要手动注册
        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);

        // 注册全局键盘事件分发器（可截获发往任意组件的按键，解决InfoWindow聚焦后快捷键失效的问题）
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
        mv.setFocusable(true);
        requestFocusInMapView();

        // 创建并显示信息窗口
        infoWindow = new MaalausInfoWindow(this);  // 自动传递this中的ButtonListener实现部分
        updateInfoWindowPosition(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y);
        infoWindow.setVisible(true);
        infoWindow.rebuildSecInfo(session.getSubMode());
        infoWindow.updateSecInfoValues(session.getSubMode().extractDisplayData(session.getStartAnchor(), session.getPendingControlPoints()));

        // 监听会话属性变更以刷新预览和窗口
        session.addPropertyChangeListener(sessionListener);
    }

    /**
     * 退出模式所必要的操作，但插件中不能直接调用（否则重复退出），应调用leaveMode，JOSM切换模式后执行本函数。
     */
    @Override
    public void exitMode() {
        // 关闭信息窗口
        if (infoWindow != null) {
            infoWindow.setVisible(false);
            infoWindow.dispose();
            infoWindow = null;
        }

        MapView mv = MainApplication.getMap().mapView;
        mv.removeTemporaryLayer(previewer);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
        mv.removeMouseListener(this);
        mv.removeMouseMotionListener(this);
        previewer.clear();

        // 移除监听器，防止累加
        session.removePropertyChangeListener(sessionListener);

        session.reset();
        super.exitMode();
    }

    /**
     * 通过 JOSM 的模式切换流程离开当前模式，避免直接重复调用exitMode。
     */
    private void leaveMode() {
        MainApplication.getMap().selectMapMode(MainApplication.getMap().mapModeSelect);
    }

    // ----------------------------------------------------------------
    // UserEventListener 实现（信息窗口按钮事件）
    // ----------------------------------------------------------------

    /**
     * 仅INFO状态下按添加曲段按钮触发
     * 根据子面板输入框中的参数（lastDisplayData，输入框修改时窗口通过监听器调用onSecInputChanged传来的data）生成所有控制点，清空并重算
     */
    @Override
    public void onAddCurveSec() {
        // TODO：检查参数合规性
        if (session.getState() != MaalausState.INFO || lastDisplayData == null) return;

        List<ColumbinaEN> allCPs = session.getSubMode().generateAllControlPoints(session.getStartAnchor(), lastDisplayData);
        if (allCPs.isEmpty()) return;

        service.clearPendingControlPoints();
        for (ColumbinaEN cp : allCPs) {
            service.addControlPoint(cp);
        }
        service.confirmSec();
        service.setPreviewPoint(null);
    }

    @Override
    public void onUndo() {
        service.undoLastSec();
        service.pauseDrawing();  // 点击按钮时保持INFO状态
    }

    @Override
    public void onCommit() {
        if (!service.commitAll(20))
            UtilsUI.warnInfo(I18n.tr("Cannot draw the line."));
    }

    @Override
    public void onCancel() {
        service.abort();
    }

    @Override
    public void onSecInputChanged(SecDisplayData data) {
        if (session.getState() != MaalausState.INFO) return;

        // 保存最近一次输入的完整参数，供onAddCurveSec使用
        this.lastDisplayData = data;

        // 由参数生成全部控制点，用最后一个作为预览点
        List<ColumbinaEN> allCPs = session.getSubMode().generateAllControlPoints(session.getStartAnchor(), data);
        if (allCPs.isEmpty()) return;

        service.setPreviewPoint(allCPs.get(allCPs.size() - 1));
        // setPreviewPoint会触发"previewPoint" PCE → sessionListener → refreshPreview + repaint
    }


    // ----------------------------------------------------------------
    // 鼠标事件（MapMode已实现MouseListener和MouseMotionListener）
    // ----------------------------------------------------------------

    @Override
    public void mouseMoved(MouseEvent event) {
        MapView mv = MainApplication.getMap().mapView;
        if (!mv.isActiveLayerVisible() || !isEditableDataLayer(mv.getLayerManager().getActiveLayer()))
            return;

        ColumbinaEN mouseEN = getColumbinaEN(event, mv);

        // 更新信息窗口位置、子模式信息面板
        if (session.getState() != MaalausState.INFO && infoWindow != null && infoWindow.isVisible()) {
            updateInfoWindowPosition(event.getXOnScreen(), event.getYOnScreen());
            // 需要只差1个控制点时才在输入框计算预览参数，此时参数通过已确定的待提交控制点+最后一个跟随鼠标滑动的待提交控制点决定
            // TODO：未来如果存在只需要部分控制点就可以先行计算部分参数的模式，则考虑在updateSecInfoValues里面做判断，并计算可计算的参数
            if (session.getPendingControlPoints().size() == session.getSubMode().getRequiredPointCount() - 1) {
                List<ColumbinaEN> pendingControlPointsForUpdate = new ArrayList<>(session.getPendingControlPoints());
                pendingControlPointsForUpdate.add(session.getPreviewPoint());
                infoWindow.updateSecInfoValues(session.getSubMode().extractDisplayData(session.getStartAnchor(), pendingControlPointsForUpdate));
            }
        }
        // 根据状态更新画布预览
        if (session.getState() == MaalausState.DRAW) {
            service.setPreviewPoint(mouseEN);
            // 这里不需要刷新，因为setPreviewPoint内会触发状态监听器，里面会刷新
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getButton() != MouseEvent.BUTTON1) return;

        MapView mv = MainApplication.getMap().mapView;
        if (!mv.isActiveLayerVisible() || !isEditableDataLayer(mv.getLayerManager().getActiveLayer()))
            return;
        ColumbinaEN clickEN = getColumbinaEN(event, mv);
        MaalausState state = session.getState();

        switch (state) {
            case INIT:
                // 首次点击：设置起点和切线方向，进入收集状态
                ColumbinaEN initialTangent = new ColumbinaEN(1, 0);  // TODO：如果凭空开始，可能没有切线方向，需要手动指定
                service.startDrawing(clickEN, initialTangent);
                break;
            case DRAW:
                // 添加待提交控制点并尝试确认当前段
                service.addControlPoint(clickEN);
                service.confirmSec();  // 如果当前待提交控制点足够（直线1个、曲线多个等），就产生新曲段，否则无操作
                service.setPreviewPoint(null);
                break;
            default:
                break;
        }
    }

    // ----------------------------------------------------------------
    // 键盘事件（全局KeyEventDispatcher，MapMode未实现）
    // ----------------------------------------------------------------

    private class MaalausKeyDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            // 只处理按下事件，释放和输入事件由正常的组件事件流处理
            if (event.getID() != KeyEvent.KEY_PRESSED) return false;

            // 获取当前焦点所属组件，判断是否为文本编辑器
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

            switch (event.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:  // 取消绘制（始终截获）
                    service.abort();
                    return true;

                case KeyEvent.VK_ENTER:  // 完成绘制（始终截获）
                    if (!service.commitAll(20))
                        UtilsUI.warnInfo(I18n.tr("Cannot draw the line."));
                    return true;

                case KeyEvent.VK_SPACE:  // 切换 DRAW/INFO（始终截获）
                    if (session.getState() != MaalausState.INFO) service.pauseDrawing();
                    else service.continueDrawing();
                    return true;

                case KeyEvent.VK_BACK_SPACE:  // 撤销上一段（文本编辑器中放行）
                    if (focusOwner instanceof JTextComponent) {
                        return false;  // 让文本编辑器正常处理退格
                    }
                    service.undoLastSec();
                    return true;

                case KeyEvent.VK_TAB:  // 切换子模式
                    // TODO：切换子模式
                    return true;

                default:
                    return false;  // 放行所有其他按键
            }
        }
    }

    // ----------------------------------------------------------------
    // 预览刷新
    // ----------------------------------------------------------------

    /**
     * 根据当前会话状态刷新预览
     */
    private void refreshPreview() {
        ColumbinaEN start = session.getStartAnchor();
        if (start == null) {
            previewer.setStartPoint(null);
            previewer.setPreview(null, null);
            return;
        }

        // 起点标记
        previewer.setStartPoint(start);

        // 已完成段的预览点列
        List<ColumbinaEN> committed = AbstractCurveSec.sampleAll(session.getSecs(), 2.0);
        previewer.setCommittedPoints(committed);

        // 当前段预览：从起点到鼠标位置的直线
        // TODO：未来扩展模式后重构为由曲段根据参数计算需要渲染的预览直线/曲线的（静态？）方法，
        //  这里改为调用模式对应的曲段的根据参数需要计算渲染对象的方法
        //  抽象曲段类添加这个计算预览方法，具体曲段实施
        List<ColumbinaEN> pending = session.getPendingControlPoints();
        ColumbinaEN previewTarget = !pending.isEmpty()
            ? pending.get(pending.size() - 1)
            : session.getPreviewPoint();
        if (previewTarget != null) {
            List<ColumbinaEN> previewPoints = generateLinePreview(start, previewTarget);
            List<ColumbinaEN> previewControls = new ArrayList<>();
            previewControls.add(previewTarget);
            previewer.setPreview(previewPoints, previewControls);
        } else {
            previewer.setPreview(null, null);
        }
    }

    /**
     * 生成直线预览点列（起点到终点之间的等分点）
     */
    private static List<ColumbinaEN> generateLinePreview(ColumbinaEN start, ColumbinaEN end) {
        List<ColumbinaEN> points = new ArrayList<>();
        double dist = start.distance(end);
        if (dist <= 0) {
            points.add(start);
            return points;
        }
        int segments = Math.max(1, (int) Math.ceil(dist / 5.0));
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            points.add(new ColumbinaEN(start.interpolate(end, t)));
        }
        return points;
    }

    /**
     * 初始化信息窗口位置，避免首次显示时停在屏幕左上角。
     */
    private void initInfoWindowPosition() {
        if (infoWindow == null) return;

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo != null) {
            Point point = pointerInfo.getLocation();
            updateInfoWindowPosition(point.x, point.y);
            return;
        }

        MapView mv = MainApplication.getMap().mapView;
        Point mapOnScreen = mv.getLocationOnScreen();
        updateInfoWindowPosition(mapOnScreen.x + mv.getWidth() / 2, mapOnScreen.y + mv.getHeight() / 2);
    }

    /**
     * 将信息窗口放到鼠标附近。
     */
    private void updateInfoWindowPosition(int screenX, int screenY) {
        if (infoWindow != null) {
            infoWindow.reposition(screenX, screenY);
        }
    }

    // ----------------------------------------------------------------
    // 工具
    // ----------------------------------------------------------------

    /**
     * 将鼠标事件位置转换为 ColumbinaEN 坐标
     */
    private static ColumbinaEN getColumbinaEN(MouseEvent e, MapView mv) {
        LatLon ll = mv.getLatLon(e.getX(), e.getY());
        return new ColumbinaEN(mv.getProjection().latlon2eastNorth(ll));
    }

    // ----------------------------------------------------------------
    // Getter（供外部访问）
    // ----------------------------------------------------------------

    public MaalausSessionData getSession() { return session; }

    public Previewer getPreviewPainter() { return previewer; }
}