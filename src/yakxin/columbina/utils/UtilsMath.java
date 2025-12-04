package yakxin.columbina.utils;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaException;

public class UtilsMath {
    /// 坐标转换
    public static EastNorth toEastNorth(LatLon ll) {  // 经纬度转距离坐标
        return ProjectionRegistry.getProjection().latlon2eastNorth(ll);
    }

    public static LatLon toLatLon(EastNorth en) {  // 距离坐标转经纬度
        return ProjectionRegistry.getProjection().eastNorth2latlon(en);
    }

    public static double surfaceDistanceToEastNorth(double surfaceDistance, double lat) throws ColumbinaException {
        double cosLat = Math.cos(Math.toRadians(lat));
        // 避免cosLat为0（极点附近）
        if (Math.abs(cosLat) < 1e-9) {
            throw new ColumbinaException(I18n.tr("Too near the pole, failed to calculate EN distance."));
        }
        return surfaceDistance / cosLat;
    }

    public static double eastNorthDistanceToSurface(double enDistance, double lat) {
        double cosLat = Math.cos(Math.toRadians(lat));
        return enDistance * cosLat;
    }

    /// 向量数学
    public static double norm(double[] v) { return Math.hypot(v[0], v[1]); }  // 模长

    public static double dot(double[] a, double[] b) { return a[0]*b[0] + a[1]*b[1]; }  // 点积

    public static double[] sub(double[] a, double[] b) { return new double[]{a[0]-b[0], a[1]-b[1]}; }  // 减法

    public static double[] add(double[] a, double[] b) { return new double[]{a[0]+b[0], a[1]+b[1]}; }  // 加法

    public static double[] mul(double[] a, double s) { return new double[]{a[0]*s, a[1]*s}; }  // 缩放
}
