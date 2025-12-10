package yakxin.columbina.features.transitionCurve;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.inputs.ColumbinaInput;

public final class TransitionCurvePreference extends AbstractPreference<TransitionCurveParams> {
    public TransitionCurvePreference() {readPreference();}

    // static {readPreference();}

    private static double transitionCurveRadius;
    private static double transitionCurveLength;
    private static double transitionChainageLength;
    private static boolean transitionCopyTag;
    private static boolean transitionDeleteOldWays;
    private static boolean transitionSelectNewWays;

    // 默认值
    public static final double DEFAULT_TRANSITION_CURVE_RADIUS = 500.0;
    public static final double DEFAULT_TRANSITION_CURVE_LENGTH = 100.0;
    public static final double DEFAULT_TRANSITION_CHAINAGE_LENGTH = 10.0;

    // 读取和储存
    public static void readPreference() {
        transitionCurveRadius = Config.getPref().getDouble(
                "columbina.transition-curve.surfaceRadius",
                DEFAULT_TRANSITION_CURVE_RADIUS
        );
        transitionCurveLength = Config.getPref().getDouble(
                "columbina.transition-curve.length",
                DEFAULT_TRANSITION_CURVE_LENGTH
        );
        transitionChainageLength = Config.getPref().getDouble(
                "columbina.transition-curve.chainage-length",
                DEFAULT_TRANSITION_CHAINAGE_LENGTH
        );
        transitionCopyTag = Config.getPref().getBoolean(
                "columbina.transition-curve.need-copy-tags",
                true
        );
        transitionDeleteOldWays = Config.getPref().getBoolean(
                "columbina.transition-curve.need-del-old-ways",
                false
        );
        transitionSelectNewWays = Config.getPref().getBoolean(
                "columbina.transition-curve.need-slc-new-ways",
                true
        );
    }

    public static void setPreferenceFromDialog(TransitionCurveDialog dlg) {
        transitionCurveRadius = dlg.getTransitionRadius();
        transitionCurveLength = dlg.getTransitionLength();
        transitionChainageLength = dlg.getChainageLength();
        transitionCopyTag = dlg.getIfCopyTag();
        transitionDeleteOldWays = dlg.getIfDeleteOld();
        transitionSelectNewWays = dlg.getIfSelectNew();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.transition-curve.surfaceRadius", transitionCurveRadius);
        Config.getPref().putDouble("columbina.transition-curve.length", transitionCurveLength);
        Config.getPref().putDouble("columbina.transition-curve.chainage-length", transitionChainageLength);
        Config.getPref().putBoolean("columbina.transition-curve.need-copy-tags", transitionCopyTag);
        Config.getPref().putBoolean("columbina.transition-curve.need-del-old-ways", transitionDeleteOldWays);
        Config.getPref().putBoolean("columbina.transition-curve.need-slc-new-ways", transitionSelectNewWays);
    }

    // getter和setter
    public static double getTransitionCurveRadius() {
        return transitionCurveRadius;
    }

    public static void setTransitionCurveRadius(double transitionCurveRadius) {
        TransitionCurvePreference.transitionCurveRadius = transitionCurveRadius;
    }

    public static double getTransitionCurveLength() {
        return transitionCurveLength;
    }

    public static void setTransitionCurveLength(double transitionCurveLength) {
        TransitionCurvePreference.transitionCurveLength = transitionCurveLength;
    }

    public static double getTransitionChainageLength() {
        return transitionChainageLength;
    }

    public static void setTransitionChainageLength(double transitionChainageLength) {
        TransitionCurvePreference.transitionChainageLength = transitionChainageLength;
    }

    public static boolean isTransitionCopyTag() {
        return transitionCopyTag;
    }

    public static void setTransitionCopyTag(boolean transitionCopyTag) {
        TransitionCurvePreference.transitionCopyTag = transitionCopyTag;
    }

    public static boolean isTransitionDeleteOldWays() {
        return transitionDeleteOldWays;
    }

    public static void setTransitionDeleteOldWays(boolean transitionDeleteOldWays) {
        TransitionCurvePreference.transitionDeleteOldWays = transitionDeleteOldWays;
    }

    public static boolean isTransitionSelectNewWays() {
        return transitionSelectNewWays;
    }

    public static void setTransitionSelectNewWays(boolean transitionSelectNewWays) {
        TransitionCurvePreference.transitionSelectNewWays = transitionSelectNewWays;
    }

    /**
     * 弹窗并保存、返回参数
     * @return 输入的参数
     */
    @Override
    public TransitionCurveParams getParamsAndUpdatePreference(ColumbinaInput input) {
        TransitionCurveDialog dialog = new TransitionCurveDialog();

        if (dialog.getValue() != 1) return null;  // 用户取消

        double radius = dialog.getTransitionRadius();
        if (radius <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid curve surfaceRadius, should be greater than 0m."));

        double length = dialog.getTransitionLength();
        if (length <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid transition curve length, should be greater than 0m."));

        double chainage = dialog.getChainageLength();
        if (chainage <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid chainage length, should be greater than 0m."));

        if (chainage > length)
            throw new IllegalArgumentException(I18n.tr("Invalid transition curve length and chainage length. The curve length should be grater chainage length."));

        // 保存设置
        TransitionCurvePreference.setPreferenceFromDialog(dialog);  // 更新自身
        TransitionCurvePreference.savePreference();
        return new TransitionCurveParams(
                transitionCurveRadius, transitionCurveLength, transitionChainageLength,
                transitionDeleteOldWays, transitionSelectNewWays, transitionCopyTag
        );
    }
}


