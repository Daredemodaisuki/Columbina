package yakxin.columbina.abstractClasses;

import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

public abstract class AbstractGenerator <ParamType extends AbstractParams> {
    /// 具体类必须实现的
    /**
     * 对于一组输入要素，调用具体算法产生绘制单条新路径所需指令
     * <p>将由action类调用，注意由于输入要素为ColumbinaSingleInput，具体生成器类需要自行判断、提取需要的要素
     * <p>如果从ColumbinaSingleInput提取的内容发现不对，暂定和没有生成指令一样先返回null
     * @param input 输入要素
     * @param params 输入参数（extends AbstractGenerator时指定的参数类）
     * @return 结果
     */
    public abstract ColumbinaSingleOutput getNewNodeWayForSingleInput(ColumbinaSingleInput input, ParamType params);  // 内部注意类型检查！

    public AbstractGenerator() {}
}


