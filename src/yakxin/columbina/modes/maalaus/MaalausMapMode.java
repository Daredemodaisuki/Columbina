package yakxin.columbina.modes.maalaus;

import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;

import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.curveSection.CurveSecUtils;
import yakxin.columbina.utils.UtilsUI;

/**
 * Maalaus 绘制模式的 MapMode
 * <p>处理鼠标和键盘事件，协调 {@link MaalausController}、{@link PreviewPainter}
 * 和 {@link MaalausInfoWindow} 之间的交互。
 * <p>当前仅实现直线延伸（LINE_EXTEND）模式。
 */
public class MaalausMapMode extends MapMode implements MaalausInfoWindow.ButtonListener {

    private final MaalausController controller;
    private final PreviewPainter previewPainter;
    private final KeyListener keyHandler;
    private final java.beans.PropertyChangeListener controllerListener;
    private MaalausInfoWindow infoWindow;

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
        this.controller = new MaalausController();
        this.previewPainter = new PreviewPainter();
        this.keyHandler = new MaalausKeyHandler();
        
        // 定义统一的监听器
        this.controllerListener = evt -> {
            // 如果完成或取消则退出模式
            if ("state".equals(evt.getPropertyName())) {
                MaalausState newState = (MaalausState) evt.getNewValue();
                if (newState == MaalausState.DONE || newState == MaalausState.ABORT) {
                    leaveMode();
                    return;
                }
            }
            // 更新信息面板内容
            if (infoWindow != null) {
                infoWindow.updateMode(controller.getSubMode().name());
                infoWindow.updateStatus(controller.getState().name(), controller.getState().getStatusInfo());
                infoWindow.updateSecCount(controller.getSecs().size());
                infoWindow.updateInfo(controller.getSubMode().getInfo());
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

        // 显式重置控制器状态，防止上次退出时可能存在的状态污染
        controller.reset();

        // 注册预览绘制器
        mv.addTemporaryLayer(previewPainter);

        // MapMode不会自动接收MapView的鼠标事件，需要手动注册
        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);

        // 注册键盘监听
        mv.addKeyListener(keyHandler);
        mv.setFocusable(true);
        requestFocusInMapView();

        // 创建并显示信息窗口
        infoWindow = new MaalausInfoWindow(this);
        infoWindow.setVisible(true);
        initInfoWindowPosition();

        // 监听控制器属性变更以刷新预览和窗口
        controller.addPropertyChangeListener(controllerListener);
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
            // UtilsUI.testMsgWindow("Closed");
        }

        MapView mv = MainApplication.getMap().mapView;
        mv.removeTemporaryLayer(previewPainter);
        mv.removeKeyListener(keyHandler);
        mv.removeMouseListener(this);
        mv.removeMouseMotionListener(this);
        previewPainter.clear();

        // 移除监听器，防止累加
        controller.removePropertyChangeListener(controllerListener);

        controller.reset();
        super.exitMode();
    }
    
    /**
     * 通过 JOSM 的模式切换流程离开当前模式，避免直接重复调用exitMode。
     */
    private void leaveMode() {
        MainApplication.getMap().selectMapMode(MainApplication.getMap().mapModeSelect);
    }

    // ----------------------------------------------------------------
    // ButtonListener 实现（信息窗口按钮事件）
    // ----------------------------------------------------------------

    @Override
    public void onUndo() {
        controller.undoLastSec();
        controller.pauseDrawing();  // 点击按钮时保持INFO状态
    }

    @Override
    public void onCommit() {
        if (!controller.commitAll(20))
            UtilsUI.warnInfo(I18n.tr("Cannot draw the line."));
    }

    @Override
    public void onCancel() {
        controller.abort();
    }


    // ----------------------------------------------------------------
    // 鼠标事件（MapMode 已实现 MouseListener + MouseMotionListener）
    // ----------------------------------------------------------------

    @Override
    public void mouseMoved(MouseEvent e) {
        MapView mv = MainApplication.getMap().mapView;
        if (!mv.isActiveLayerVisible() || !isEditableDataLayer(mv.getLayerManager().getActiveLayer()))
            return;

        ColumbinaEN mouseEN = getColumbinaEN(e, mv);

        // 更新信息窗口位置
        if (controller.getState() != MaalausState.INFO && infoWindow != null && infoWindow.isVisible()) {
            updateInfoWindowPosition(e.getXOnScreen(), e.getYOnScreen());
        }
        // 根据状态更新预览
        if (controller.getState() == MaalausState.DRAW) {
            controller.setPreviewPoint(mouseEN);
            refreshPreview();
            mv.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        
        MapView mv = MainApplication.getMap().mapView;
        if (!mv.isActiveLayerVisible() || !isEditableDataLayer(mv.getLayerManager().getActiveLayer()))
            return;
        ColumbinaEN clickEN = getColumbinaEN(e, mv);
        MaalausState state = controller.getState();
        
        switch (state) {
            case INIT:
                // 首次点击：设置起点和切线方向，进入收集状态
                ColumbinaEN initialTangent = new ColumbinaEN(1, 0);
                controller.startDrawing(clickEN, initialTangent);
                // TODO：如果凭空开始，可能没有切线方向，需要手动指定
                break;
            case DRAW:
                // 添加控制点并确认当前段
                controller.addControlPoint(clickEN);
                controller.confirmSec();
                controller.setPreviewPoint(null);
                break;
            default:
                break;
        }
    }

    // ----------------------------------------------------------------
    // 键盘事件（内部 KeyListener）
    // ----------------------------------------------------------------

    private class MaalausKeyHandler implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER:  // 完成绘制
                    if (!controller.commitAll(20))
                        UtilsUI.warnInfo(I18n.tr("Cannot draw the line."));
                    break;
                case KeyEvent.VK_ESCAPE:  // 取消绘制
                    controller.abort();
                    break;
                case KeyEvent.VK_BACK_SPACE:  // 撤销上一段
                    controller.undoLastSec();
                    break;
                case KeyEvent.VK_SPACE:
                    if (controller.getState() != MaalausState.INFO) controller.pauseDrawing();
                    else controller.continueDrawing();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // 未使用
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // 未使用
        }
    }

    // ----------------------------------------------------------------
    // 预览刷新
    // ----------------------------------------------------------------

    /**
     * 根据当前控制器状态刷新预览
     */
    private void refreshPreview() {
        ColumbinaEN start = controller.getStartAnchor();
        if (start == null) {
            previewPainter.setStartPoint(null);
            previewPainter.setPreview(null, null);
            return;
        }

        // 起点标记
        previewPainter.setStartPoint(start);

        // 已完成段的预览点列
        List<ColumbinaEN> committed = CurveSecUtils.sampleAll(controller.getSecs(), 2.0);
        previewPainter.setCommittedPoints(committed);

        // 当前段预览：从起点到鼠标位置的直线
        List<ColumbinaEN> pending = controller.getPendingControlPoints();
        ColumbinaEN previewTarget = !pending.isEmpty()
            ? pending.get(pending.size() - 1)
            : controller.getPreviewPoint();
        if (previewTarget != null) {
            List<ColumbinaEN> previewPoints = generateLinePreview(start, previewTarget);
            List<ColumbinaEN> previewControls = new ArrayList<>();
            previewControls.add(previewTarget);
            previewPainter.setPreview(previewPoints, previewControls);
        } else {
            previewPainter.setPreview(null, null);
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

    public MaalausController getController() { return controller; }

    public PreviewPainter getPreviewPainter() { return previewPainter; }
}
