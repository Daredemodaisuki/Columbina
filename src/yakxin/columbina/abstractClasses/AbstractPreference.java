package yakxin.columbina.abstractClasses;

import yakxin.columbina.data.ColumbinaPrefItem;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

public abstract class AbstractPreference <ParamType extends AbstractParams> {
    protected AbstractPreference(ColumbinaPrefItem<?>[] allPrefItems) {
        this.ALL = allPrefItems;
        readPreference();
    }
    
    protected final ColumbinaPrefItem<?>[] ALL;
    
    public void readPreference() {
        for (ColumbinaPrefItem<?> item : ALL) item.readFromConfig();
    }
    
    public void savePreference() {
        for (ColumbinaPrefItem<?> item : ALL) item.saveToConfig();
    }
    
    /**
     * 弹窗获取用户输入，校验后保存并返回参数
     * @param input 总输入
     * @return 参数
     */
    public abstract ParamType getParamsAndUpdatePreference(ColumbinaInput input);
}


