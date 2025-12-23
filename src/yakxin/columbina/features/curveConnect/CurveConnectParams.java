package yakxin.columbina.features.curveConnect;

import yakxin.columbina.abstractClasses.AbstractParams;

public final class CurveConnectParams extends AbstractParams {
    public final double surfaceCircleRadius;
    public final double surfaceTransArcLength;
    public final double surfaceChainageLength;
    public final int dirMode;
    public final boolean ableToAdjustInputNode;

    CurveConnectParams(
            double surfaceCircleRadius, double surfaceTransArcLength,
            double surfaceChainageLength, int dirMode, boolean ableToAdjustInputNode,
            boolean selectNew
    ) {
        super(selectNew);
        this.surfaceCircleRadius = surfaceCircleRadius;
        this.surfaceTransArcLength = surfaceTransArcLength;
        this.surfaceChainageLength = surfaceChainageLength;
        this.dirMode = dirMode;
        this.ableToAdjustInputNode = ableToAdjustInputNode;
    }
}


