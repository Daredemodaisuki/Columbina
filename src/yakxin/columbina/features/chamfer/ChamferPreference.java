package yakxin.columbina.features.chamfer;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.inputs.ColumbinaInput;

public final class ChamferPreference extends AbstractPreference<ChamferParams> {
    public ChamferPreference() {readPreference();}

    // static {readPreference();}

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

    /**
     * 弹窗并保存、返回参数
     * @return 输入的参数
     */
    @Override
    public ChamferParams getParamsAndUpdatePreference(ColumbinaInput input) {
        ChamferDialog chamferDialog = new ChamferDialog();
        if (chamferDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1

        int mode = chamferDialog.getChamferMode();

        double distanceA = chamferDialog.getChamferDistanceA();
        if (distanceA <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer distance BA, should be greater than 0m."));

        double distanceC = chamferDialog.getChamferDistanceC();
        if (mode == ChamferGenerator.USING_DISTANCE) {
            if (distanceC <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer distance BC, should be greater than 0m."));
        }

        double angleADeg = chamferDialog.getChamferAngleADeg();
        if (mode == ChamferGenerator.USING_ANGLE_A) {
            if (angleADeg <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer angle A, should be greater than 0m."));
        }

        // 保存设置
        ChamferPreference.setPreferenceFromDialog(chamferDialog);  // 更新自身
        ChamferPreference.savePreference();
        return new ChamferParams(
                chamferMode,
                chamferDistanceA, chamferDistanceC, chamferAngleADeg,
                chamferDeleteOldWays, chamferSelectNewWays, chamferCopyTag
        );
    }
}


