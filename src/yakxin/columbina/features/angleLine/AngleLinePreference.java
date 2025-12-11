package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

public final class AngleLinePreference extends AbstractPreference<AngleLineParams> {
    public AngleLinePreference() {
        readPreference();
    }

    private static double angleLineAngleDeg;
    private static double angleLineLength;
    private static boolean angleLineSelectNewWays;

    public static final double DEFAULT_ANGLE_LINE_ANGLE_DEG = 45.0;
    public static final double DEFAULT_ANGLE_LINE_LENGTH = 100.0;

    public static void readPreference() {
        angleLineAngleDeg = Config.getPref().getDouble("columbina.angle-line.angle-deg", DEFAULT_ANGLE_LINE_ANGLE_DEG);
        angleLineLength = Config.getPref().getDouble("columbina.angle-line.length", DEFAULT_ANGLE_LINE_LENGTH);
        angleLineSelectNewWays = Config.getPref().getBoolean("columbina.angle-line.need-copy-tags", true);
    }

    public static void setPreferenceFromDialog(AngleLineDialog dlg) {
        angleLineAngleDeg = dlg.getAngleLineAngleDeg();
        angleLineLength = dlg.getAngleLineLength();
        angleLineSelectNewWays = dlg.getIfSelectNew();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.angle-line.angle-deg", angleLineAngleDeg);
        Config.getPref().putDouble("columbina.angle-line.length", angleLineLength);
        Config.getPref().putBoolean("columbina.angle-line.need-copy-tags", angleLineSelectNewWays);
    }

    public static double getAngleLineAngleDeg() {
        return angleLineAngleDeg;
    }

    public static void setAngleLineAngleDeg(double angleLineAngleDeg) {
        AngleLinePreference.angleLineAngleDeg = angleLineAngleDeg;
    }

    public static double getAngleLineLength() {
        return angleLineLength;
    }

    public static void setAngleLineLength(double angleLineLength) {
        AngleLinePreference.angleLineLength = angleLineLength;
    }

    public static boolean isAngleLineSelectNewWays() {
        return angleLineSelectNewWays;
    }

    public static void setAngleLineSelectNewWays(boolean angleLineSelectNewWays) {
        AngleLinePreference.angleLineSelectNewWays = angleLineSelectNewWays;
    }

    /**
     * 弹窗并保存、返回参数
     * @return 输入的参数
     */
    @Override
    public AngleLineParams getParamsAndUpdatePreference(ColumbinaInput input) {
        AngleLineDialog angleLineDialog = new AngleLineDialog();
        if (angleLineDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1

        // 数值检查
        double LineLength = angleLineDialog.getAngleLineLength();
        if (LineLength <= 0) throw new IllegalArgumentException(I18n.tr("Invalid length, should be greater than 0m."));

        // 保存设置
        AngleLinePreference.setPreferenceFromDialog(angleLineDialog);  // 更新自身
        AngleLinePreference.savePreference();
        return new AngleLineParams(
                angleLineAngleDeg, angleLineLength,
                angleLineSelectNewWays
        );
    }
}


