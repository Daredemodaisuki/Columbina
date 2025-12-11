package yakxin.columbina.features.chamfer;

import yakxin.columbina.abstractClasses.AbstractParams;

public final class ChamferParams extends AbstractParams {
    public final int mode;
    public final double surfaceDistanceA;
    public final double surfaceDistanceC;
    public final double angleADeg;

    ChamferParams(
            int mode,
            double surfaceDistanceA, double surfaceDistanceC,
            double angleADeg,
            boolean deleteOld, boolean selectNew, boolean copyTag
    ) {
        super(deleteOld, selectNew, copyTag);
        this.mode = mode;
        this.surfaceDistanceA = surfaceDistanceA;
        this.surfaceDistanceC = surfaceDistanceC;
        this.angleADeg = angleADeg;
    }
}


