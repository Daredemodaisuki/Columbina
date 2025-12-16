package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.spi.preferences.Config;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

public class CurveConnectPreference extends AbstractPreference<CurveConnectParams> {
    public CurveConnectPreference() {readPreference();}

    private static double curveConnectRadius;
    private static double curveConnectTransArcLength;
    private static double curveConnectChainageLength;
    private static int curveConnectMaxPointPerArc;
    private static int curveConnectLeftRight;
    private static boolean curveConnectSelectNewWays;

    public static final double DEFAULT_CURVE_CONNECT_RADIUS = 114.5;
    public static final double DEFAULT_CURVE_CONNECT_TRANS_ARC_LENGTH = 51.45;
    public static final double DEFAULT_CURVE_CONNECT_CHAINAGE_LENGTH = 8.10;
    public static final int DEFAULT_CURVE_CONNECT_MAX_POINT_PER_ARC = 20;
    public static final int DEFAULT_CURVE_CONNECT_LEFT_RIGHT = CurveConnectGenerator.COUNTER_CLOCKWISE_MODE;

    public static void readPreference() {
        curveConnectRadius = Config.getPref().getDouble("columbina.curve-connect.radius", DEFAULT_CURVE_CONNECT_RADIUS);
        curveConnectTransArcLength = Config.getPref().getDouble("columbina.curve-connect.trans-arc-length", DEFAULT_CURVE_CONNECT_TRANS_ARC_LENGTH);
        curveConnectChainageLength = Config.getPref().getDouble("columbina.curve-connect.chainage-length", DEFAULT_CURVE_CONNECT_CHAINAGE_LENGTH);
        curveConnectMaxPointPerArc = Config.getPref().getInt("columbina.curve-connect.max-num-of-point", DEFAULT_CURVE_CONNECT_MAX_POINT_PER_ARC);
        curveConnectLeftRight = Config.getPref().getInt("columbina.curve-connect.left-right", DEFAULT_CURVE_CONNECT_LEFT_RIGHT);
        curveConnectSelectNewWays = Config.getPref().getBoolean("columbina.curve-connect.need-copy-tags", true);
    }

    public static double getCurveConnectRadius() {
        return curveConnectRadius;
    }

    public static double getCurveConnectTransArcLength() {
        return curveConnectTransArcLength;
    }

    public static boolean isCurveConnectSelectNewWays() {
        return curveConnectSelectNewWays;
    }

    public static double getCurveConnectChainageLength() {
        return curveConnectChainageLength;
    }

    public static int getCurveConnectMaxPointPerArc() {
        return curveConnectMaxPointPerArc;
    }

    public static int getCurveConnectLeftRight() {
        return curveConnectLeftRight;
    }

    @Override
    public CurveConnectParams getParamsAndUpdatePreference(ColumbinaInput input) {
        return null;
    }
}
