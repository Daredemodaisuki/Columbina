package yakxin.columbina.abstractClasses;

import yakxin.columbina.data.dto.DrawingNewNodeResult;

public abstract class AbstractGenerator <ParamType extends AbstractParams> {
    /// 具体类必须实现的
    /**
     * 对于一组输入要素，用于产生绘制单条新路径所需指令
     * <p>将由action类调用，注意由于输入要素为Object类型，具体生成器类需要自行判断、转换为需要的类型
     * @param input 输入要素
     * @param params 输入参数（extends AbstractGenerator时指定的参数类）
     * @return 结果
     */
    public abstract DrawingNewNodeResult getNewNodeWayForSingleInput(Object input, ParamType params);  // 内部注意类型检查！

    public AbstractGenerator() {}
}
