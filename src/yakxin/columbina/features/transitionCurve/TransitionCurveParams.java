package yakxin.columbina.features.transitionCurve;

import yakxin.columbina.abstractClasses.AbstractParams;

public final class TransitionCurveParams extends AbstractParams {
    public final double surfaceRadius;
    public final double surfaceTransArcLength;
    public final double surfaceChainageLength;
    TransitionCurveParams(
            double surfaceRadius, double surfaceTransArcLength, double surfaceChainageLength,
            boolean deleteOld, boolean selectNew, boolean copyTag
    ) {
        super(deleteOld, selectNew, copyTag);
        this.surfaceRadius = surfaceRadius;
        this.surfaceTransArcLength = surfaceTransArcLength;
        this.surfaceChainageLength = surfaceChainageLength;
    }
}


