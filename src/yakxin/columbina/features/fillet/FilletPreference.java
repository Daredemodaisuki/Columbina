package yakxin.columbina.features.fillet;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.utils.UtilsUI;

public final class FilletPreference extends AbstractPreference<FilletParams>  {
    public FilletPreference() {
        readPreference();
    }

    // static {readPreference();}

    private static double filletRadius;
    private static double filletChainageLength;
    private static int filletMaxPointPerArc;
    private static double filletMinAngleDeg;
    private static double filletMaxAngleDeg;
    private static boolean filletCopyTag;
    private static boolean filletDeleteOldWays;
    private static boolean filletSelectNewWays;

    // 默认值
    public static final double DEFAULT_FILLET_RADIUS = 150.0;
    public static final double DEFAULT_FILLET_CHAINAGE_LENGTH = 15.0;
    public static final int DEFAULT_FILLET_MAX_POINT_PER_ARC = 20;
    public static final double DEFAULT_FILLET_MIN_ANGLE_DEG = 2.5;
    public static final double DEFAULT_FILLET_MAX_ANGLE_DEG = 177.5;

    // 读取和储存
    public static void readPreference() {
        filletRadius = Config.getPref().getDouble("columbina.round-corner.surfaceRadius", DEFAULT_FILLET_RADIUS);
        filletChainageLength = Config.getPref().getDouble("columbina.round-corner.chainage-length", DEFAULT_FILLET_CHAINAGE_LENGTH);
        filletMaxPointPerArc = Config.getPref().getInt("columbina.round-corner.max-num-of-point", DEFAULT_FILLET_MAX_POINT_PER_ARC);
        filletMinAngleDeg = Config.getPref().getDouble("columbina.round-corner.min-angle-deg", DEFAULT_FILLET_MIN_ANGLE_DEG);
        filletMaxAngleDeg = Config.getPref().getDouble("columbina.round-corner.max-angle-deg", DEFAULT_FILLET_MAX_ANGLE_DEG);
        filletCopyTag = Config.getPref().getBoolean("columbina.round-corner.need-copy-tags", true);
        filletDeleteOldWays = Config.getPref().getBoolean("columbina.round-corner.need-del-old-ways", false);
        filletSelectNewWays = Config.getPref().getBoolean("columbina.round-corner.need-slc-new-ways", true);
    }

    public static void setPreferenceFromDialog(FilletDialog dlg) {
        filletRadius = dlg.getFilletRadius();
        filletChainageLength = dlg.getFilletChainageLength();
        filletMaxPointPerArc = dlg.getFilletMaxPointNum();
        filletMinAngleDeg = dlg.getMinAngleDeg();
        filletMaxAngleDeg = dlg.getMaxAngleDeg();
        filletCopyTag = dlg.getIfCopyTag();
        filletDeleteOldWays = dlg.getIfDeleteOld();
        filletSelectNewWays = dlg.getIfSelectNew();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.round-corner.surfaceRadius", filletRadius);
        Config.getPref().putDouble("columbina.round-corner.chainage-length", filletChainageLength);
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

    public static double getFilletChainageLength() {
        return filletChainageLength;
    }

    public static void setFilletChainageLength(double filletChainageLength) {
        FilletPreference.filletChainageLength = filletChainageLength;
    }

    /**
     * 弹窗并校验、保存、返回参数
     * @return 输入的参数
     */
    @Override
    public FilletParams getParamsAndUpdatePreference(ColumbinaInput input) {
        FilletDialog filletDialog = new FilletDialog(input);  // 创建设置对话框

        if (filletDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1

        double radius = filletDialog.getFilletRadius();  // 圆角半径
        if (radius <= 0.0) throw new IllegalArgumentException(I18n.tr("Invalid round corner surfaceRadius, should be greater than 0m."));

        double chainageLength = filletDialog.getFilletChainageLength();  // 桩距（节点距离）
        if (chainageLength < 0.1) {
            chainageLength = 0.1;
            filletDialog.setFilletChainageLength(0.1);
            UtilsUI.warnInfo(I18n.tr("Minimum chainage length (node spacing) for round corner should be at least 0.1m, set to 0.1m."));
        }
        // else if (chainageLength < 10.0) UtilsUI.warnInfo(I18n.tr("Chainage length (node spacing) is too small, the nodes may be dense."));

        int maxPointNum = filletDialog.getFilletMaxPointNum();  // 曲线点数
        if (maxPointNum < 1) throw new IllegalArgumentException(I18n.tr("Invalid maximum number of points for round corner, should be at least 1."));
        else if (maxPointNum < 5) UtilsUI.warnInfo(I18n.tr("Maximum number of points for round corner is too low, the result may not be ideal."));

        double minAngleDeg = filletDialog.getMinAngleDeg();  // 最小张角
        if (minAngleDeg < 0.0) {
            minAngleDeg = 0.0;
            filletDialog.setMinAngleDeg(0.0);
            UtilsUI.warnInfo(I18n.tr("Minimum angle should be at least 0°, set to 0°."));
        }

        double maxAngleDeg = filletDialog.getMaxAngleDeg();  // 最大张角
        if (maxAngleDeg > 180.0) {
            maxAngleDeg = 180.0;
            filletDialog.setMaxAngleDeg(180.0);
            UtilsUI.warnInfo(I18n.tr("Maximum angle should be at most 180°, set to 180°."));
        }

        // 保存设置
        FilletPreference.setPreferenceFromDialog(filletDialog);  // 更新自身
        FilletPreference.savePreference();
        return new FilletParams(
                filletRadius, filletChainageLength, filletMaxPointPerArc,
                filletMinAngleDeg, filletMaxAngleDeg,
                filletDeleteOldWays, filletSelectNewWays, filletCopyTag
        );
    }
}


