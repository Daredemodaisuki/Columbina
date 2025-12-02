package yakxin.columbina.data;

import org.openstreetmap.josm.spi.preferences.Config;
import yakxin.columbina.RoundCornersDialog;

public final class Preference {
    private Preference() {
    }  // 不让创建实例？

    static {
        readPreference();
    }

    private static double radius;
    private static double angleStep;
    private static int maxPointPerArc;
    private static double minAngleDeg;
    private static double maxAngleDeg;
    private static boolean copyTag;
    private static boolean deleteOldWays;
    private static boolean selectNewWays;

    // 默认值
    public static final double DEFAULT_RADIUS = 150.0;
    public static final double DEFAULT_ANGLE_STEP = 0.75;
    public static final int DEFAULT_MAX_POINT_PER_ARC = 20;
    public static final double DEFAULT_MIN_ANGLE_DEG = 2.5;
    public static final double DEFAULT_MAX_ANGLE_DEG = 177.5;

    public static double getRadius() {
        return radius;
    }

    public static void setRadius(double radius) {
        Preference.radius = radius;
    }

    public static int getMaxPointPerArc() {
        return maxPointPerArc;
    }

    public static void setMaxPointPerArc(int maxPointPerArc) {
        Preference.maxPointPerArc = maxPointPerArc;
    }

    public static boolean isCopyTag() {
        return copyTag;
    }

    public static void setCopyTag(boolean copyTag) {
        Preference.copyTag = copyTag;
    }

    public static boolean isDeleteOldWays() {
        return deleteOldWays;
    }

    public static void setDeleteOldWays(boolean deleteOldWays) {
        Preference.deleteOldWays = deleteOldWays;
    }

    public static boolean isSelectNewWays() {
        return selectNewWays;
    }

    public static void setSelectNewWays(boolean selectNewWays) {
        Preference.selectNewWays = selectNewWays;
    }

    public static void readPreference() {
        radius = Config.getPref().getDouble("columbina.round-corner.radius", DEFAULT_RADIUS);
        angleStep = Config.getPref().getDouble("columbina.round-corner.angle-step", DEFAULT_ANGLE_STEP);
        maxPointPerArc = Config.getPref().getInt("columbina.round-corner.max-num-of-point", DEFAULT_MAX_POINT_PER_ARC);
        minAngleDeg = Config.getPref().getDouble("columbina.round-corner.min-angle-deg", DEFAULT_MIN_ANGLE_DEG);
        maxAngleDeg = Config.getPref().getDouble("columbina.round-corner.max-angle-deg", DEFAULT_MAX_ANGLE_DEG);
        copyTag = Config.getPref().getBoolean("columbina.round-corner.need-copy-tags", true);
        deleteOldWays = Config.getPref().getBoolean("columbina.round-corner.need-del-old-ways", false);
        selectNewWays = Config.getPref().getBoolean("columbina.round-corner.need-slc-new-ways", true);
    }

    public static void setPreferenceFromDialog(RoundCornersDialog dlg) {
        radius = dlg.getFilletRadius();
        angleStep = dlg.getFilletAngleStep();
        maxPointPerArc = dlg.getFilletMaxPointNum();
        minAngleDeg = dlg.getMinAngleDeg();
        maxAngleDeg = dlg.getMaxAngleDeg();
        copyTag = dlg.getIfCopyTag();
        deleteOldWays = dlg.getIfDeleteOld();
        selectNewWays = dlg.getIfSelectNew();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.round-corner.radius", radius);
        Config.getPref().putDouble("columbina.round-corner.angle-step", angleStep);
        Config.getPref().putInt("columbina.round-corner.max-num-of-point", maxPointPerArc);
        Config.getPref().putDouble("columbina.round-corner.min-angle-deg", minAngleDeg);
        Config.getPref().putDouble("columbina.round-corner.max-angle-deg", maxAngleDeg);
        Config.getPref().putBoolean("columbina.round-corner.need-copy-tags", copyTag);
        Config.getPref().putBoolean("columbina.round-corner.need-del-old-ways", deleteOldWays);
        Config.getPref().putBoolean("columbina.round-corner.need-slc-new-ways", selectNewWays);
    }

    public static double getMinAngleDeg() {
        return minAngleDeg;
    }

    public static void setMinAngleDeg(double minAngleDeg) {
        Preference.minAngleDeg = minAngleDeg;
    }

    public static double getMaxAngleDeg() {
        return maxAngleDeg;
    }

    public static void setMaxAngleDeg(double maxAngleDeg) {
        Preference.maxAngleDeg = maxAngleDeg;
    }

    public static double getAngleStep() {
        return angleStep;
    }

    public static void setAngleStep(double angleStep) {
        Preference.angleStep = angleStep;
    }
}


