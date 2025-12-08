package yakxin.columbina.features.transitionCurve;

import yakxin.columbina.abstractClasses.AbstractParams;

public final class TransitionCurveParams extends AbstractParams {
    public final double surfaceRadius;
    public final double surfaceTransArcLength;
    public final double chainageNum;
    TransitionCurveParams(
            double surfaceRadius, double surfaceTransArcLength, double chainageNum,
            boolean deleteOld, boolean selectNew, boolean copyTag
    ) {
        super(deleteOld, selectNew, copyTag);
        this.surfaceRadius = surfaceRadius;
        this.surfaceTransArcLength = surfaceTransArcLength;
        this.chainageNum = chainageNum;
    }
}
