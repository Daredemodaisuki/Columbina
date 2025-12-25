package yakxin.columbina.features.fillet;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.ColumbinaPrefItem;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.utils.UtilsUI;

public final class FilletPreference extends AbstractPreference<FilletParams> {
    public FilletPreference() {
        super(new ColumbinaPrefItem[] {
                RADIUS, CHAINAGE_LENGTH,
                MAX_POINT_PER_ARC, MIN_ANGLE_DEG, MAX_ANGLE_DEG,
                NEED_COPY_TAGS, NEED_DELETE_OLD, NEED_SELECT_NEW
        });
    }
    
    public static final String PREF_NAME = "round-corner";
    
    // 默认值
    public static final double DEFAULT_FILLET_RADIUS = 150.0;
    public static final double DEFAULT_FILLET_CHAINAGE_LENGTH = 15.0;
    public static final int DEFAULT_FILLET_MAX_POINT_PER_ARC = 20;
    public static final double DEFAULT_FILLET_MIN_ANGLE_DEG = 2.5;
    public static final double DEFAULT_FILLET_MAX_ANGLE_DEG = 177.5;
    
    private static final ColumbinaPrefItem<Double>  RADIUS            = new ColumbinaPrefItem<>(PREF_NAME, "radius", Double.class, DEFAULT_FILLET_RADIUS);
    private static final ColumbinaPrefItem<Double>  CHAINAGE_LENGTH   = new ColumbinaPrefItem<>(PREF_NAME, "chainage-length", Double.class, DEFAULT_FILLET_CHAINAGE_LENGTH);
    private static final ColumbinaPrefItem<Integer> MAX_POINT_PER_ARC = new ColumbinaPrefItem<>(PREF_NAME, "max-num-of-point", Integer.class, DEFAULT_FILLET_MAX_POINT_PER_ARC);
    private static final ColumbinaPrefItem<Double>  MIN_ANGLE_DEG     = new ColumbinaPrefItem<>(PREF_NAME, "min-angle-deg", Double.class, DEFAULT_FILLET_MIN_ANGLE_DEG);
    private static final ColumbinaPrefItem<Double>  MAX_ANGLE_DEG     = new ColumbinaPrefItem<>(PREF_NAME, "max-angle-deg", Double.class, DEFAULT_FILLET_MAX_ANGLE_DEG);
    private static final ColumbinaPrefItem<Boolean> NEED_COPY_TAGS    = new ColumbinaPrefItem<>(PREF_NAME, "need-copy-tags", Boolean.class, true);
    private static final ColumbinaPrefItem<Boolean> NEED_DELETE_OLD   = new ColumbinaPrefItem<>(PREF_NAME, "need-del-old-ways", Boolean.class, false);
    private static final ColumbinaPrefItem<Boolean> NEED_SELECT_NEW   = new ColumbinaPrefItem<>(PREF_NAME, "need-slc-new-ways", Boolean.class, true);
    
    @Override
    public FilletParams getParamsAndUpdatePreference(ColumbinaInput input) {
        readPreference();
        // 从当前配置创建参数对象
        FilletParams savedParams = new FilletParams(
                RADIUS.getValue(), CHAINAGE_LENGTH.getValue(),
                MAX_POINT_PER_ARC.getValue(), MIN_ANGLE_DEG.getValue(), MAX_ANGLE_DEG.getValue(),
                NEED_DELETE_OLD.getValue(), NEED_SELECT_NEW.getValue(), NEED_COPY_TAGS.getValue()
        );
        
        FilletDialog filletDialog = new FilletDialog(input, savedParams);
        if (filletDialog.getValue() != 1) return null;  // 用户取消
        
        // 直接从对话框获取参数进行验证
        FilletParams newParams = filletDialog.getParams();
        
        // 参数验证
        if (newParams.surfaceRadius <= 0.0)
            throw new IllegalArgumentException(I18n.tr("Invalid round corner radius, should be greater than 0m."));
        
        double chainageLengthChecked = newParams.surfaceChainageLength;
        if (chainageLengthChecked < 0.1) {
            chainageLengthChecked = 0.1;
            UtilsUI.warnInfo(I18n.tr("Minimum chainage length (node spacing) for round corner should be at least 0.1m, set to 0.1m."));
        }
        
        if (newParams.maxPointNum < 1)
            throw new IllegalArgumentException(I18n.tr("Invalid maximum number of points for round corner, should be at least 1."));
        else if (newParams.maxPointNum < 5)
            UtilsUI.warnInfo(I18n.tr("Maximum number of points for round corner is too low, the result may not be ideal."));
        
        double minAngleDegChecked = newParams.minAngleDeg;
        if (minAngleDegChecked < 0.0) {
            minAngleDegChecked = 0.0;
            UtilsUI.warnInfo(I18n.tr("Minimum angle should be at least 0°, set to 0°."));
        }
        
        double maxAngleDegChecked = newParams.maxAngleDeg;
        if (maxAngleDegChecked > 180.0) {
            maxAngleDegChecked = 180.0;
            UtilsUI.warnInfo(I18n.tr("Maximum angle should be at most 180°, set to 180°."));
        }
        
        // 更新配置项
        RADIUS.setValue(newParams.surfaceRadius);
        CHAINAGE_LENGTH.setValue(chainageLengthChecked);
        MAX_POINT_PER_ARC.setValue(newParams.maxPointNum);
        MIN_ANGLE_DEG.setValue(minAngleDegChecked);
        MAX_ANGLE_DEG.setValue(maxAngleDegChecked);
        NEED_DELETE_OLD.setValue(newParams.deleteOld);
        NEED_SELECT_NEW.setValue(newParams.selectNew);
        NEED_COPY_TAGS.setValue(newParams.copyTag);
        
        savePreference();
        return new FilletParams(
                newParams.surfaceRadius, chainageLengthChecked,
                newParams.maxPointNum, minAngleDegChecked, maxAngleDegChecked,
                newParams.deleteOld, newParams.selectNew, newParams.copyTag
        );
    }
}


