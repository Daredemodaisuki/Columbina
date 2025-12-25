package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.ColumbinaPrefItem;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

public final class CurveConnectPreference extends AbstractPreference<CurveConnectParams> {
    public CurveConnectPreference() {
        super(new ColumbinaPrefItem[] {
                RADIUS, TRANS_ARC_LEN, CHAINAGE_LEN, DIRECTION_MODE, ABLE_MOD_ENDS, NEED_SELECT_NEW
        });
    }
    
    public static final String PREF_NAME = "curve-connect";
    
    // 默认值
    public static final double DEFAULT_CURVE_CONNECT_RADIUS = 114.5;
    public static final double DEFAULT_CURVE_CONNECT_TRANS_ARC_LENGTH = 51.45;
    public static final double DEFAULT_CURVE_CONNECT_CHAINAGE_LENGTH = 8.10;
    public static final int DEFAULT_CURVE_CONNECT_DIR_MODE = CurveConnectGenerator.COUNTER_CLOCKWISE_MODE;
    
    private static final ColumbinaPrefItem<Double>  RADIUS          = new ColumbinaPrefItem<>(PREF_NAME,"radius", Double.class, DEFAULT_CURVE_CONNECT_RADIUS);
    private static final ColumbinaPrefItem<Double>  TRANS_ARC_LEN   = new ColumbinaPrefItem<>(PREF_NAME,"trans-arc-length", Double.class, DEFAULT_CURVE_CONNECT_TRANS_ARC_LENGTH);
    private static final ColumbinaPrefItem<Double>  CHAINAGE_LEN    = new ColumbinaPrefItem<>(PREF_NAME,"chainage-length", Double.class, DEFAULT_CURVE_CONNECT_CHAINAGE_LENGTH);
    private static final ColumbinaPrefItem<Integer> DIRECTION_MODE  = new ColumbinaPrefItem<>(PREF_NAME,"direction-mode", Integer.class, DEFAULT_CURVE_CONNECT_DIR_MODE);
    private static final ColumbinaPrefItem<Boolean> ABLE_MOD_ENDS   = new ColumbinaPrefItem<>(PREF_NAME,"able-to-modify-ends", Boolean.class, true);
    private static final ColumbinaPrefItem<Boolean> NEED_SELECT_NEW = new ColumbinaPrefItem<>(PREF_NAME,"slc-new-ways", Boolean.class, true);
    
    @Override
    public CurveConnectParams getParamsAndUpdatePreference(ColumbinaInput input) {
        readPreference();
        CurveConnectDialog curveConnectDialog = new CurveConnectDialog(new CurveConnectParams(
                RADIUS.getValue(), TRANS_ARC_LEN.getValue(), CHAINAGE_LEN.getValue(),
                DIRECTION_MODE.getValue(), ABLE_MOD_ENDS.getValue(),
                NEED_SELECT_NEW.getValue()
        ));
        if (curveConnectDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1
        
        CurveConnectParams newParams = curveConnectDialog.getParams();
        
        // 数值检查
        if (newParams.surfaceCircleRadius <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid curve radius, should be greater than 0m."));
        // TODO：桩距检查逻辑
        
        // 更新配置项
        RADIUS.setValue(newParams.surfaceCircleRadius);
        TRANS_ARC_LEN.setValue(newParams.surfaceTransArcLength);
        CHAINAGE_LEN.setValue(newParams.surfaceChainageLength);
        DIRECTION_MODE.setValue(newParams.dirMode);
        ABLE_MOD_ENDS.setValue(newParams.ableToAdjustInputNode);
        NEED_SELECT_NEW.setValue(newParams.selectNew);
        
        savePreference();
        return newParams;
    }
}


