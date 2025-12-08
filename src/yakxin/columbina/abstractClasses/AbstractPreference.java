package yakxin.columbina.abstractClasses;

public abstract class AbstractPreference <ParamType extends AbstractParams> {
    public abstract ParamType getParams();  // 弹窗并保存、返回参数
}
