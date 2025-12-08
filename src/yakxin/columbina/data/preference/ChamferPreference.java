package yakxin.columbina.data.preference;

import org.openstreetmap.josm.spi.preferences.Config;
import yakxin.columbina.features.chamfer.ChamferDialog;
import yakxin.columbina.features.chamfer.ChamferGenerator;

public final class ChamferPreference {
    private ChamferPreference() {}

    static {readPreference();}

    private static double chamferDistanceA;
    private static double chamferDistanceC;
    private static double chamferAngleADeg;
    private static boolean chamferCopyTag;
    private static boolean chamferDeleteOldWays;
    private static boolean chamferSelectNewWays;

    private static int chamferMode;

    public static final double DEFAULT_CHAMFER_DISTANCE_A = 100;
    public static final double DEFAULT_CHAMFER_DISTANCE_C = 100;
    public static final double DEFAULT_CHAMFER_ANGLE_A_DEG = 51.4;
    public static final int DEFAULT_CHAMFER_MODE = ChamferGenerator.USING_DISTANCE;

    // 读取和储存
    public static void readPreference() {
        chamferDistanceA = Config.getPref().getDouble("columbina.chamfer.distance-A", DEFAULT_CHAMFER_DISTANCE_A);
        chamferDistanceC = Config.getPref().getDouble("columbina.chamfer.distance-C", DEFAULT_CHAMFER_DISTANCE_C);
        chamferAngleADeg = Config.getPref().getDouble("columbina.chamfer.angle-A-deg", DEFAULT_CHAMFER_ANGLE_A_DEG);
        chamferCopyTag = Config.getPref().getBoolean("columbina.chamfer.need-copy-tags", true);
        chamferDeleteOldWays = Config.getPref().getBoolean("columbina.chamfer.need-del-old-ways", false);
        chamferSelectNewWays = Config.getPref().getBoolean("columbina.chamfer.need-slc-new-ways", true);
        chamferMode = Config.getPref().getInt("columbina.chamfer.mode", DEFAULT_CHAMFER_MODE);
    }

    public static void setPreferenceFromDialog(ChamferDialog dlg) {
        chamferDistanceA = dlg.getChamferDistanceA();
        chamferDistanceC = dlg.getChamferDistanceC();
        chamferAngleADeg = dlg.getChamferAngleADeg();
        chamferCopyTag = dlg.getIfCopyTag();
        chamferDeleteOldWays = dlg.getIfDeleteOld();
        chamferSelectNewWays = dlg.getIfSelectNew();
        chamferMode = dlg.getChamferMode();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.chamfer.distance-A", chamferDistanceA);
        Config.getPref().putDouble("columbina.chamfer.distance-C", chamferDistanceC);
        Config.getPref().putDouble("columbina.chamfer.angle-A-deg", chamferAngleADeg);
        Config.getPref().putBoolean("columbina.chamfer.need-copy-tags", chamferCopyTag);
        Config.getPref().putBoolean("columbina.chamfer.need-del-old-ways", chamferDeleteOldWays);
        Config.getPref().putBoolean("columbina.chamfer.need-slc-new-ways", chamferSelectNewWays);
        Config.getPref().putInt("columbina.chamfer.mode", chamferMode);
    }

    // getter和setter
    public static double getChamferDistanceA() {
        return chamferDistanceA;
    }

    public static void setChamferDistanceA(double chamferDistanceA) {
        ChamferPreference.chamferDistanceA = chamferDistanceA;
    }

    public static double getChamferDistanceC() {
        return chamferDistanceC;
    }

    public static void setChamferDistanceC(double chamferDistanceC) {
        ChamferPreference.chamferDistanceC = chamferDistanceC;
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

    public static int getChamferMode() {
        return chamferMode;
    }

    public static void setChamferMode(int chamferMode) {
        ChamferPreference.chamferMode = chamferMode;
    }
}


