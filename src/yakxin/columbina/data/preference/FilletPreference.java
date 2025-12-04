package yakxin.columbina.data.preference;

import org.openstreetmap.josm.spi.preferences.Config;
import yakxin.columbina.roundCorner.FilletDialog;

public final class FilletPreference {
    private FilletPreference() {
    }  // 不让创建实例？

    static {
        readPreference();
    }

    private static double filletRadius;
    private static double filletAngleStep;
    private static int filletMaxPointPerArc;
    private static double filletMinAngleDeg;
    private static double filletMaxAngleDeg;
    private static boolean filletCopyTag;
    private static boolean filletDeleteOldWays;
    private static boolean filletSelectNewWays;

    // 默认值
    public static final double DEFAULT_FILLET_RADIUS = 150.0;
    public static final double DEFAULT_FILLET_ANGLE_STEP = 0.75;
    public static final int DEFAULT_FILLET_MAX_POINT_PER_ARC = 20;
    public static final double DEFAULT_FILLET_MIN_ANGLE_DEG = 2.5;
    public static final double DEFAULT_FILLET_MAX_ANGLE_DEG = 177.5;

    // 读取和储存
    public static void readPreference() {
        filletRadius = Config.getPref().getDouble("columbina.round-corner.radius", DEFAULT_FILLET_RADIUS);
        filletAngleStep = Config.getPref().getDouble("columbina.round-corner.angle-step", DEFAULT_FILLET_ANGLE_STEP);
        filletMaxPointPerArc = Config.getPref().getInt("columbina.round-corner.max-num-of-point", DEFAULT_FILLET_MAX_POINT_PER_ARC);
        filletMinAngleDeg = Config.getPref().getDouble("columbina.round-corner.min-angle-deg", DEFAULT_FILLET_MIN_ANGLE_DEG);
        filletMaxAngleDeg = Config.getPref().getDouble("columbina.round-corner.max-angle-deg", DEFAULT_FILLET_MAX_ANGLE_DEG);
        filletCopyTag = Config.getPref().getBoolean("columbina.round-corner.need-copy-tags", true);
        filletDeleteOldWays = Config.getPref().getBoolean("columbina.round-corner.need-del-old-ways", false);
        filletSelectNewWays = Config.getPref().getBoolean("columbina.round-corner.need-slc-new-ways", true);
    }

    public static void setPreferenceFromDialog(FilletDialog dlg) {
        filletRadius = dlg.getFilletRadius();
        filletAngleStep = dlg.getFilletAngleStep();
        filletMaxPointPerArc = dlg.getFilletMaxPointNum();
        filletMinAngleDeg = dlg.getMinAngleDeg();
        filletMaxAngleDeg = dlg.getMaxAngleDeg();
        filletCopyTag = dlg.getIfCopyTag();
        filletDeleteOldWays = dlg.getIfDeleteOld();
        filletSelectNewWays = dlg.getIfSelectNew();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.round-corner.radius", filletRadius);
        Config.getPref().putDouble("columbina.round-corner.angle-step", filletAngleStep);
        Config.getPref().putInt("columbina.round-corner.max-num-of-point", filletMaxPointPerArc);
        Config.getPref().putDouble("columbina.round-corner.min-angle-deg", filletMinAngleDeg);
        Config.getPref().putDouble("columbina.round-corner.max-angle-deg", filletMaxAngleDeg);
        Config.getPref().putBoolean("columbina.round-corner.need-copy-tags", filletCopyTag);
        Config.getPref().putBoolean("columbina.round-corner.need-del-old-ways", filletDeleteOldWays);
        Config.getPref().putBoolean("columbina.round-corner.need-slc-new-ways", filletSelectNewWays);
    }

    // getter和setter
    public static double getFilletRadius() {
        return filletRadius;
    }

    public static void setFilletRadius(double filletRadius) {
        FilletPreference.filletRadius = filletRadius;
    }

    public static int getFilletMaxPointPerArc() {
        return filletMaxPointPerArc;
    }

    public static void setFilletMaxPointPerArc(int filletMaxPointPerArc) {
        FilletPreference.filletMaxPointPerArc = filletMaxPointPerArc;
    }

    public static boolean isFilletCopyTag() {
        return filletCopyTag;
    }

    public static void setFilletCopyTag(boolean filletCopyTag) {
        FilletPreference.filletCopyTag = filletCopyTag;
    }

    public static boolean isFilletDeleteOldWays() {
        return filletDeleteOldWays;
    }

    public static void setFilletDeleteOldWays(boolean filletDeleteOldWays) {
        FilletPreference.filletDeleteOldWays = filletDeleteOldWays;
    }

    public static boolean isFilletSelectNewWays() {
        return filletSelectNewWays;
    }

    public static void setFilletSelectNewWays(boolean filletSelectNewWays) {
        FilletPreference.filletSelectNewWays = filletSelectNewWays;
    }

    public static double getFilletMinAngleDeg() {
        return filletMinAngleDeg;
    }

    public static void setFilletMinAngleDeg(double filletMinAngleDeg) {
        FilletPreference.filletMinAngleDeg = filletMinAngleDeg;
    }

    public static double getFilletMaxAngleDeg() {
        return filletMaxAngleDeg;
    }

    public static void setFilletMaxAngleDeg(double filletMaxAngleDeg) {
        FilletPreference.filletMaxAngleDeg = filletMaxAngleDeg;
    }

    public static double getFilletAngleStep() {
        return filletAngleStep;
    }

    public static void setFilletAngleStep(double filletAngleStep) {
        FilletPreference.filletAngleStep = filletAngleStep;
    }
}


