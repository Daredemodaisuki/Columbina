package yakxin.columbina.features.transitionCurve;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.ColumbinaPrefItem;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

public final class TransitionCurvePreference extends AbstractPreference<TransitionCurveParams> {
    public TransitionCurvePreference() {
        super(new ColumbinaPrefItem[] {
                RADIUS, TRANS_ARC_LEN, CHAINAGE_LEN, NEED_COPY_TAGS, NEED_DELETE_OLD, NEED_SELECT_NEW
        });
    }
    
    public static final String PREF_NAME = "transition-curve";
    
    // 默认值
    public static final double DEFAULT_TRANSITION_CURVE_RADIUS = 500.0;
    public static final double DEFAULT_TRANSITION_CURVE_LENGTH = 100.0;
    public static final double DEFAULT_TRANSITION_CHAINAGE_LENGTH = 10.0;
    
    private static final ColumbinaPrefItem<Double>  RADIUS          = new ColumbinaPrefItem<>(PREF_NAME, "radius", Double.class, DEFAULT_TRANSITION_CURVE_RADIUS);
    private static final ColumbinaPrefItem<Double>  TRANS_ARC_LEN   = new ColumbinaPrefItem<>(PREF_NAME, "trans-arc-length", Double.class, DEFAULT_TRANSITION_CURVE_LENGTH);
    private static final ColumbinaPrefItem<Double>  CHAINAGE_LEN    = new ColumbinaPrefItem<>(PREF_NAME, "chainage-length", Double.class, DEFAULT_TRANSITION_CHAINAGE_LENGTH);
    private static final ColumbinaPrefItem<Boolean> NEED_COPY_TAGS  = new ColumbinaPrefItem<>(PREF_NAME, "need-copy-tags", Boolean.class, true);
    private static final ColumbinaPrefItem<Boolean> NEED_DELETE_OLD = new ColumbinaPrefItem<>(PREF_NAME, "need-del-old-ways", Boolean.class, false);
    private static final ColumbinaPrefItem<Boolean> NEED_SELECT_NEW = new ColumbinaPrefItem<>(PREF_NAME, "need-slc-new-ways", Boolean.class, true);
    
    /**
     * 弹窗并保存、返回参数
     * @return 输入的参数
     */
    @Override
    public TransitionCurveParams getParamsAndUpdatePreference(ColumbinaInput input) {
        readPreference();
        TransitionCurveDialog transitionCurveDialog = new TransitionCurveDialog(new TransitionCurveParams(
                RADIUS.getValue(), TRANS_ARC_LEN.getValue(), CHAINAGE_LEN.getValue(),
                NEED_DELETE_OLD.getValue(), NEED_SELECT_NEW.getValue(), NEED_COPY_TAGS.getValue()
        ));
        if (transitionCurveDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1
        
        TransitionCurveParams newParams = transitionCurveDialog.getParams();
        
        if (newParams.surfaceRadius <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid curve radius, should be greater than 0m."));
        
        if (newParams.surfaceTransArcLength <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid transition curve length, should be greater than 0m."));
        
        if (newParams.surfaceChainageLength <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid chainage length, should be greater than 0m."));
        
        if (newParams.surfaceChainageLength > newParams.surfaceTransArcLength)
            throw new IllegalArgumentException(I18n.tr("Invalid transition curve length and chainage length. The curve length should be grater chainage length."));
        
        // 更新参数
        RADIUS.setValue(newParams.surfaceRadius);
        TRANS_ARC_LEN.setValue(newParams.surfaceTransArcLength);
        CHAINAGE_LEN.setValue(newParams.surfaceChainageLength);
        NEED_COPY_TAGS.setValue(newParams.copyTag);
        NEED_DELETE_OLD.setValue(newParams.deleteOld);
        NEED_SELECT_NEW.setValue(newParams.selectNew);
        
        savePreference();
        return newParams;
    }
}