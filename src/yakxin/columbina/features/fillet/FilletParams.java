package yakxin.columbina.features.fillet;

import yakxin.columbina.abstractThings.AbstractParams;

public class FilletParams extends AbstractParams {
    public final double surfaceRadius;
    public final double angleStep;
    public final int maxPointNum;
    public final double minAngleDeg;
    public final double maxAngleDeg;

    public FilletParams(
            double surfaceRadius, double angleStep, int maxPointNum,
            double minAngleDeg, double maxAngleDeg,
            boolean deleteOld, boolean selectNew, boolean copyTag
    ) {
        this.surfaceRadius = surfaceRadius;
        this.angleStep = angleStep;
        this.maxPointNum = maxPointNum;
        this.minAngleDeg = minAngleDeg;
        this.maxAngleDeg = maxAngleDeg;
        this.deleteOld = deleteOld;
        this.selectNew = selectNew;
        this.copyTag = copyTag;
    }
}
