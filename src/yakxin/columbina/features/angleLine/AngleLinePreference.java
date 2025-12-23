package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.ColumbinaPrefItem;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.utils.UtilsMath;
import yakxin.columbina.utils.UtilsUI;

public final class AngleLinePreference extends AbstractPreference<AngleLineParams> {
    public AngleLinePreference() {
        super(new ColumbinaPrefItem[] {
                ANGLE_DEG, LENGTH, NEED_SELECT_NEW
        });
    }
    
    public static final String PREF_NAME = "angle-line";
    
    // 默认值
    public static final double DEFAULT_ANGLE_LINE_ANGLE_DEG = 45.0;
    public static final double DEFAULT_ANGLE_LINE_LENGTH = 100.0;
    
    private static final ColumbinaPrefItem<Double>  ANGLE_DEG       = new ColumbinaPrefItem<>(PREF_NAME, "angle-deg", Double.class, DEFAULT_ANGLE_LINE_ANGLE_DEG);
    private static final ColumbinaPrefItem<Double>  LENGTH          = new ColumbinaPrefItem<>(PREF_NAME, "length", Double.class, DEFAULT_ANGLE_LINE_LENGTH);
    private static final ColumbinaPrefItem<Boolean> NEED_SELECT_NEW = new ColumbinaPrefItem<>(PREF_NAME, "slc-new-ways", Boolean.class, true);
    
    @Override
    public AngleLineParams getParamsAndUpdatePreference(ColumbinaInput input) {
        readPreference();
        AngleLineParams savedParams = new AngleLineParams(
                ANGLE_DEG.getValue(), LENGTH.getValue(),
                NEED_SELECT_NEW.getValue()
        );
        
        AngleLineDialog angleLineDialog = new AngleLineDialog(savedParams);
        if (angleLineDialog.getValue() != 1) return null;
        
        AngleLineParams newParams = angleLineDialog.getParams();
        
        // 数值检查
        if (newParams.surfaceLength <= 0)
            throw new IllegalArgumentException(I18n.tr("Invalid length, should be greater than 0m."));
        
        double angleDegChecked = newParams.angleDeg;  // 归一化角度
        if (angleDegChecked != UtilsMath.normAngleDeg(angleDegChecked)) {
            angleDegChecked = UtilsMath.normAngleDeg(angleDegChecked);
            UtilsUI.warnInfo(I18n.tr("The angle should be with from -180° to +180°, normed to {0}°.", angleDegChecked));
        }
        
        // 更新配置项
        ANGLE_DEG.setValue(angleDegChecked);
        LENGTH.setValue(newParams.surfaceLength);
        NEED_SELECT_NEW.setValue(newParams.selectNew);
        
        savePreference();
        return new AngleLineParams(
                angleDegChecked, newParams.surfaceLength,
                newParams.selectNew
        );
    }
}


