package yakxin.columbina.data.preference;

import org.openstreetmap.josm.spi.preferences.Config;
import yakxin.columbina.chamfer.ChamferDialog;
import yakxin.columbina.utils;

public class ChamferPreference {
    private ChamferPreference() {}

    static {readPreference();}

    private static double chamferDistanceA;
    private static double chamferDistanceB;
    private static double chamferAngleADeg;
    private static boolean chamferCopyTag;
    private static boolean chamferDeleteOldWays;
    private static boolean chamferSelectNewWays;

    public static final double DEFAULT_CHAMFER_DISTANCE_A = 100;
    public static final double DEFAULT_CHAMFER_DISTANCE_B = 100;
    public static final double DEFAULT_CHAMFER_ANGLE_A_DEG = 51.4;

    // 读取和储存
    public static void readPreference() {
        chamferDistanceA = Config.getPref().getDouble("columbina.chamfer.distance-A", DEFAULT_CHAMFER_DISTANCE_A);
        chamferDistanceB = Config.getPref().getDouble("columbina.chamfer.distance-B", DEFAULT_CHAMFER_DISTANCE_B);
        chamferAngleADeg = Config.getPref().getDouble("columbina.chamfer.angle-A-deg", DEFAULT_CHAMFER_ANGLE_A_DEG);
        chamferCopyTag = Config.getPref().getBoolean("columbina.chamfer.need-copy-tags", true);
        chamferDeleteOldWays = Config.getPref().getBoolean("columbina.chamfer.need-del-old-ways", false);
        chamferSelectNewWays = Config.getPref().getBoolean("columbina.chamfer.need-slc-new-ways", true);
    }

    public static void setPreferenceFromDialog(ChamferDialog dlg) {
        chamferDistanceA = dlg.getChamferDistanceA();
        chamferDistanceB = dlg.getChamferDistanceB();
        chamferAngleADeg = dlg.getChamferAngleADeg();
        chamferCopyTag = dlg.getIfCopyTag();
        chamferDeleteOldWays = dlg.getIfDeleteOld();
        chamferSelectNewWays = dlg.getIfSelectNew();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.chamfer.distance-A", chamferDistanceA);
        Config.getPref().putDouble("columbina.chamfer.distance-B", chamferDistanceB);
        Config.getPref().putDouble("columbina.chamfer.angle-A-deg", chamferAngleADeg);
        Config.getPref().putBoolean("columbina.chamfer.need-copy-tags", chamferCopyTag);
        Config.getPref().putBoolean("columbina.chamfer.need-del-old-ways", chamferDeleteOldWays);
        Config.getPref().putBoolean("columbina.chamfer.need-slc-new-ways", chamferSelectNewWays);
    }

    // getter和setter
    public static double getChamferDistanceA() {
        return chamferDistanceA;
    }

    public static void setChamferDistanceA(double chamferDistanceA) {
        ChamferPreference.chamferDistanceA = chamferDistanceA;
    }

    public static double getChamferDistanceB() {
        return chamferDistanceB;
    }

    public static void setChamferDistanceB(double chamferDistanceB) {
        ChamferPreference.chamferDistanceB = chamferDistanceB;
    }

    public static double getChamferAngleADeg() {
        return chamferAngleADeg;
    }

    public static void setChamferAngleADeg(double chamferAngleADeg) {
        ChamferPreference.chamferAngleADeg = chamferAngleADeg;
    }

    public static boolean isChamferCopyTag() {
        return chamferCopyTag;
    }

    public static void setChamferCopyTag(boolean chamferCopyTag) {
        ChamferPreference.chamferCopyTag = chamferCopyTag;
    }

    public static boolean isChamferDeleteOldWays() {
        return chamferDeleteOldWays;
    }

    public static void setChamferDeleteOldWays(boolean chamferDeleteOldWays) {
        ChamferPreference.chamferDeleteOldWays = chamferDeleteOldWays;
    }

    public static boolean isChamferSelectNewWays() {
        return chamferSelectNewWays;
    }

    public static void setChamferSelectNewWays(boolean chamferSelectNewWays) {
        ChamferPreference.chamferSelectNewWays = chamferSelectNewWays;
    }
}


