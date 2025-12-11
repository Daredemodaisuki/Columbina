package yakxin.columbina.abstractClasses;

import yakxin.columbina.data.dto.inputs.ColumbinaInput;

public abstract class AbstractPreference <ParamType extends AbstractParams> {
    public abstract ParamType getParamsAndUpdatePreference(ColumbinaInput input);  // 弹窗并保存、返回参数
}


