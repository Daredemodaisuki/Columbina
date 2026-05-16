package yakxin.columbina.features.fillet;

import yakxin.columbina.abstractClasses.AbstractParams;
import yakxin.columbina.features.fillet.advanced.AdvFilletParams;

public final class FilletParams extends AbstractParams {
    public final double surfaceRadius;
    public final double surfaceChainageLength;
    public final int maxPointNum;
    public final double minAngleDeg;
    public final double maxAngleDeg;
    public AdvFilletParams advFilletParams = null;  // 默认不启用高级参数

    public FilletParams(
            double surfaceRadius, double surfaceChainageLength, int maxPointNum,
            double minAngleDeg, double maxAngleDeg,
            boolean deleteOld, boolean selectNew, boolean copyTag
    ) {
        super(deleteOld, selectNew, copyTag);
        this.surfaceRadius = surfaceRadius;
        this.surfaceChainageLength = surfaceChainageLength;
        this.maxPointNum = maxPointNum;
        this.minAngleDeg = minAngleDeg;
        this.maxAngleDeg = maxAngleDeg;
    }
}


