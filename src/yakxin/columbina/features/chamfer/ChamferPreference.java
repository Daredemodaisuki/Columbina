package yakxin.columbina.features.chamfer;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.ColumbinaPrefItem;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

public final class ChamferPreference extends AbstractPreference<ChamferParams> {
    public ChamferPreference() {
        super(new ColumbinaPrefItem[] {
                DISTANCE_A, DISTANCE_C, ANGLE_A_DEG, MODE, NEED_COPY_TAGS, NEED_DELETE_OLD, NEED_SELECT_NEW
        });
    }
    
    public static final String PREF_NAME = "chamfer";
    
    // 默认值
    public static final double DEFAULT_CHAMFER_DISTANCE_A = 100;
    public static final double DEFAULT_CHAMFER_DISTANCE_C = 100;
    public static final double DEFAULT_CHAMFER_ANGLE_A_DEG = 51.4;
    public static final int DEFAULT_CHAMFER_MODE = ChamferGenerator.DISTANCE_MODE;
    
    private static final ColumbinaPrefItem<Double>  DISTANCE_A       = new ColumbinaPrefItem<>(PREF_NAME, "distance-A", Double.class, DEFAULT_CHAMFER_DISTANCE_A);
    private static final ColumbinaPrefItem<Double>  DISTANCE_C       = new ColumbinaPrefItem<>(PREF_NAME, "distance-C", Double.class, DEFAULT_CHAMFER_DISTANCE_C);
    private static final ColumbinaPrefItem<Double>  ANGLE_A_DEG      = new ColumbinaPrefItem<>(PREF_NAME, "angle-A-deg", Double.class, DEFAULT_CHAMFER_ANGLE_A_DEG);
    private static final ColumbinaPrefItem<Integer> MODE             = new ColumbinaPrefItem<>(PREF_NAME, "mode", Integer.class, DEFAULT_CHAMFER_MODE);
    private static final ColumbinaPrefItem<Boolean> NEED_COPY_TAGS   = new ColumbinaPrefItem<>(PREF_NAME, "need-copy-tags", Boolean.class, true);
    private static final ColumbinaPrefItem<Boolean> NEED_DELETE_OLD  = new ColumbinaPrefItem<>(PREF_NAME, "need-del-old-ways", Boolean.class, false);
    private static final ColumbinaPrefItem<Boolean> NEED_SELECT_NEW  = new ColumbinaPrefItem<>(PREF_NAME, "need-slc-new-ways", Boolean.class, true);
    
    @Override
    public ChamferParams getParamsAndUpdatePreference(ColumbinaInput input) {
        readPreference();
        ChamferParams savedParams = new ChamferParams(
                MODE.getValue(),
                DISTANCE_A.getValue(), DISTANCE_C.getValue(),
                ANGLE_A_DEG.getValue(),
                NEED_DELETE_OLD.getValue(), NEED_SELECT_NEW.getValue(), NEED_COPY_TAGS.getValue()
        );
        
        ChamferDialog chamferDialog = new ChamferDialog(savedParams);
        if (chamferDialog.getValue() != 1) return null;
        
        ChamferParams newParams = chamferDialog.getParams();
        int mode = newParams.mode;
        
        if (newParams.surfaceDistanceA <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer distance BA, should be greater than 0m."));
        
        if (mode == ChamferGenerator.DISTANCE_MODE) {
            if (newParams.surfaceDistanceC <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer distance BC, should be greater than 0m."));
        }
        
        if (mode == ChamferGenerator.ANGLE_A_MODE) {
            if (newParams.angleADeg <= 0) throw new IllegalArgumentException(I18n.tr("Invalid round chamfer angle A, should be greater than 0m."));
        }
        
        // 更新配置项
        DISTANCE_A.setValue(newParams.surfaceDistanceA);
        DISTANCE_C.setValue(newParams.surfaceDistanceC);
        ANGLE_A_DEG.setValue(newParams.angleADeg);
        MODE.setValue(newParams.mode);
        NEED_COPY_TAGS.setValue(newParams.copyTag);
        NEED_DELETE_OLD.setValue(newParams.deleteOld);
        NEED_SELECT_NEW.setValue(newParams.selectNew);
        
        savePreference();
        return newParams;
    }
}