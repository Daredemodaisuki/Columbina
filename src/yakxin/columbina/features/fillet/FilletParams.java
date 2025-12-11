package yakxin.columbina.features.fillet;

import yakxin.columbina.abstractClasses.AbstractParams;

public final class FilletParams extends AbstractParams {
    public final double surfaceRadius;
    public final double surfaceChainageLength;
    public final int maxPointNum;
    public final double minAngleDeg;
    public final double maxAngleDeg;

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


