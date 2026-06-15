package yakxin.columbina.utils.utilsView;

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
 * <p>支持通过 {@link Renderable} 接口扩展辅助几何渲染（法线、圆心、转角线等），
 * 由子模式（{@link yakxin.columbina.modes.maalaus.MaalausSubMode}）计算几何数据，
 * Previewer 统一绘制，不感知具体子模式类型。
 */
public class Previewer implements MapViewPaintable {
    private List<ColumbinaEN> previewPoints = Collections.emptyList();  // 当前段预览点列（起点到鼠标/控制点之间的采样点）
    private List<ColumbinaEN> controlPoints = Collections.emptyList();  // 控制点标记（当前段的端控制点）
    private List<ColumbinaEN> committedPoints = Collections.emptyList();  // 已完成曲段的预览点列
    private ColumbinaEN startPoint;  // 起点标记
    private List<Renderable> renderables = Collections.emptyList();  // 辅助几何渲染原语

    // ================================================================
    // 公开设置方法（由 MaalausMapMode 在事件处理中调用）
    // ================================================================

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
     * 设置辅助几何渲染原语列表
     * <p>由子模式的 {@code calculatePreviewGeometry()} 方法计算，
     * Previewer 按列表顺序统一绘制于「已完成曲段」和「当前段预览线」之间。
     * @param renderables 渲染原语列表
     */
    public void setRenderables(List<Renderable> renderables) {
        this.renderables = renderables != null
            ? Collections.unmodifiableList(new ArrayList<>(renderables))
            : Collections.emptyList();
    }

    /**
     * 清除所有预览数据
     */
    public void clear() {
        this.previewPoints = Collections.emptyList();
        this.controlPoints = Collections.emptyList();
        this.committedPoints = Collections.emptyList();
        this.startPoint = null;
        this.renderables = Collections.emptyList();
    }

    // ================================================================
    // 渲染原语接口（Previewer 不感知子模式类型，只按接口渲染）
    // ================================================================

    /**
     * 可渲染原语：由子模式计算几何数据，通过 {@link #setRenderables(List)} 传入后统一点绘。
     */
    public interface Renderable {
        void draw(Graphics2D g, MapView mv);
    }

    /** 折线/线段渲染原语 */
    public static class RenderableLine implements Renderable {
        public final List<ColumbinaEN> points;
        public final Color color;
        public final float width;
        public final boolean dashed;

        public RenderableLine(List<ColumbinaEN> points, Color color, float width, boolean dashed) {
            this.points = points;
            this.color = color;
            this.width = width;
            this.dashed = dashed;
        }

        @Override
        public void draw(Graphics2D g, MapView mv) {
            if (points == null || points.size() < 2) return;
            if (dashed) {
                g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{Math.max(width * 2, 4f), Math.max(width * 2, 4f)}, 0));
            } else {
                g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g.setColor(color);
            Point prev = null;
            for (ColumbinaEN en : points) {
                Point p = mv.getPoint(en);
                if (prev != null) g.drawLine(prev.x, prev.y, p.x, p.y);
                prev = p;
            }
        }
    }

    /** 点标记渲染原语 */
    public static class RenderablePoint implements Renderable {
        public final ColumbinaEN point;
        public final Color color;
        public final float size;
        public final boolean filled;

        public RenderablePoint(ColumbinaEN point, Color color, float size, boolean filled) {
            this.point = point;
            this.color = color;
            this.size = size;
            this.filled = filled;
        }

        @Override
        public void draw(Graphics2D g, MapView mv) {
            if (point == null) return;
            g.setColor(color);
            Point p = mv.getPoint(point);
            int s = Math.round(size);
            if (filled) {
                g.fillOval(p.x - s / 2, p.y - s / 2, s, s);
            } else {
                g.drawOval(p.x - s / 2, p.y - s / 2, s, s);
            }
        }
    }

    // ================================================================
    // MapViewPaintable 接口
    // ================================================================

    @Override
    public void paint(Graphics2D graphics, MapView mv, Bounds bbox) {
        if (previewPoints.isEmpty() && committedPoints.isEmpty()
            && startPoint == null && renderables.isEmpty())
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

        // 2. 绘制辅助几何（法线虚线/圆心标记/转角线等）—— 由子模式计算、Previewer 统一绘制
        for (Renderable r : renderables) {
            r.draw(graphicsCopy, mv);
        }

        // 3. 绘制当前段预览线（半透明蓝色实线）
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

        // 4. 绘制起点标记（蓝色圆点）
        if (startPoint != null) {
            graphicsCopy.setColor(new Color(0, 120, 255, 220));
            Point p = mv.getPoint(startPoint);
            graphicsCopy.fillOval(p.x - 6, p.y - 6, 12, 12);
            graphicsCopy.setColor(Color.WHITE);
            graphicsCopy.setStroke(new BasicStroke(2f));
            graphicsCopy.drawOval(p.x - 6, p.y - 6, 12, 12);
        }

        // 5. 绘制控制点（红色圆点）
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