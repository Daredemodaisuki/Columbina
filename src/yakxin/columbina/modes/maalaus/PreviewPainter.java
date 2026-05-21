package yakxin.columbina.modes.maalaus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;

import yakxin.columbina.data.ColumbinaEN;

/**
 * Maalaus 模式的预览绘制器
 * <p>实现 {@link MapViewPaintable} 接口，在 MapView 上绘制控制点标记、
 * 当前段预览线（起点到鼠标/控制点）、以及已完成曲段。
 */
public class PreviewPainter implements MapViewPaintable {
    private List<ColumbinaEN> previewPoints = Collections.emptyList();  // 当前段预览点列（起点到鼠标/ 控制点之间的采样点）
    private List<ColumbinaEN> controlPoints = Collections.emptyList();  // 控制点标记（当前段的端控制点）
    private List<ColumbinaEN> committedPoints = Collections.emptyList();  // 已完成曲段的预览点列
    private ColumbinaEN startPoint;  // 起点标记

    // ----------------------------------------------------------------
    // 公开设置方法（由 MaalausMapMode 在事件处理中调用）
    // ----------------------------------------------------------------

    /**
     * 设置当前预览数据
     * @param preview 当前段预览点列
     * @param controls 控制点列表
     */
    public void setPreview(List<ColumbinaEN> preview, List<ColumbinaEN> controls) {
        this.previewPoints = preview != null
            ? Collections.unmodifiableList(new ArrayList<>(preview))
            : Collections.emptyList();
        this.controlPoints = controls != null
            ? Collections.unmodifiableList(new ArrayList<>(controls))
            : Collections.emptyList();
    }

    /**
     * 设置已完成曲段的预览点列
     * @param points 已完成段的离散点列
     */
    public void setCommittedPoints(List<ColumbinaEN> points) {
        this.committedPoints = points != null
            ? Collections.unmodifiableList(new ArrayList<>(points))
            : Collections.emptyList();
    }

    /**
     * 设置起点标记
     * @param point 起点坐标
     */
    public void setStartPoint(ColumbinaEN point) {
        this.startPoint = point;
    }

    /**
     * 清除所有预览数据
     */
    public void clear() {
        this.previewPoints = Collections.emptyList();
        this.controlPoints = Collections.emptyList();
        this.committedPoints = Collections.emptyList();
        this.startPoint = null;
    }

    // ----------------------------------------------------------------
    // MapViewPaintable 接口
    // ----------------------------------------------------------------

    @Override
    public void paint(Graphics2D graphics, MapView mv, Bounds bbox) {
        if (previewPoints.isEmpty() && committedPoints.isEmpty() && startPoint == null)
            return;

        Graphics2D graphicsCopy = (Graphics2D) graphics.create();
        graphicsCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. 绘制已完成的曲段（绿色实线）
        if (!committedPoints.isEmpty()) {
            graphicsCopy.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphicsCopy.setColor(new Color(0, 180, 80, 200));
            Point prev = null;
            for (ColumbinaEN en : committedPoints) {
                Point p = mv.getPoint(en);
                if (prev != null) graphicsCopy.drawLine(prev.x, prev.y, p.x, p.y);
                prev = p;
            }
        }

        // 2. 绘制当前段预览线（半透明蓝色实线）
        if (!previewPoints.isEmpty()) {
            graphicsCopy.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphicsCopy.setColor(new Color(0, 120, 255, 180));
            Point prev = null;
            for (ColumbinaEN en : previewPoints) {
                Point p = mv.getPoint(en);
                if (prev != null) graphicsCopy.drawLine(prev.x, prev.y, p.x, p.y);
                prev = p;
            }
        }

        // 3. 绘制起点标记（蓝色圆点）
        if (startPoint != null) {
            graphicsCopy.setColor(new Color(0, 120, 255, 220));
            Point p = mv.getPoint(startPoint);
            graphicsCopy.fillOval(p.x - 6, p.y - 6, 12, 12);
            graphicsCopy.setColor(Color.WHITE);
            graphicsCopy.setStroke(new BasicStroke(2f));
            graphicsCopy.drawOval(p.x - 6, p.y - 6, 12, 12);
        }

        // 4. 绘制控制点（红色圆点）
        if (!controlPoints.isEmpty()) {
            graphicsCopy.setColor(new Color(255, 80, 80, 200));
            for (ColumbinaEN en : controlPoints) {
                Point p = mv.getPoint(en);
                graphicsCopy.fillOval(p.x - 5, p.y - 5, 10, 10);
            }
        }

        graphicsCopy.dispose();
    }
}