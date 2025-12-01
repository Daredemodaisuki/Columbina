package yakxin.columbina.data;

import org.openstreetmap.josm.spi.preferences.Config;
import yakxin.columbina.RoundCornersDialog;

public final class Preference {
    private Preference() {
    }

    static {
        readPreference();
    }

    private static double radius;
    private static int numPoint;
    private static boolean copyTag;
    private static boolean deleteOldWays;
    private static boolean selectNewWays;

    public static double getRadius() {
        return radius;
    }

    public static void setRadius(double radius) {
        Preference.radius = radius;
    }

    public static int getNumPoint() {
        return numPoint;
    }

    public static void setNumPoint(int numPoint) {
        Preference.numPoint = numPoint;
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
        radius = Config.getPref().getDouble("columbina.round-corner.radius", 150);  // 150为默认值
        numPoint = Config.getPref().getInt("columbina.round-corner.num-of-point", 20);
        copyTag = Config.getPref().getBoolean("columbina.round-corner.need-copy-tags", true);
        deleteOldWays = Config.getPref().getBoolean("columbina.round-corner.need-del-old-ways", false);
        selectNewWays = Config.getPref().getBoolean("columbina.round-corner.need-slc-new-ways", true);
    }

    public static void setPreferenceFromDialog(RoundCornersDialog dlg) {
        radius = dlg.getFilletRadius();
        numPoint = dlg.getFilletPointNum();
        copyTag = dlg.getIfCopyTag();
        deleteOldWays = dlg.getIfDeleteOld();
        selectNewWays = dlg.getIfSelectNew();
    }

    public static void savePreference() {
        Config.getPref().putDouble("columbina.round-corner.radius", radius);
        Config.getPref().putInt("columbina.round-corner.num-of-point", numPoint);
        Config.getPref().putBoolean("columbina.round-corner.need-copy-tags", copyTag);
        Config.getPref().putBoolean("columbina.round-corner.need-del-old-ways", deleteOldWays);
        Config.getPref().putBoolean("columbina.round-corner.need-slc-new-ways", selectNewWays);
    }
}


