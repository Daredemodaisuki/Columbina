package yakxin.columbina.features.curveConnect;

import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

public class CurveConnectGenerator extends AbstractGenerator<CurveConnectParams> {
    public static final int LEFT = 0;  // 注意这个不是用来判断±1的！
    public static final int RIGHT = 1;

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, CurveConnectParams params) {
        return null;
    }


}
