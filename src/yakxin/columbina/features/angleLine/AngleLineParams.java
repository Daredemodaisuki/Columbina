package yakxin.columbina.features.angleLine;

import yakxin.columbina.abstractClasses.AbstractParams;

public final class AngleLineParams extends AbstractParams {
    public final double angleDeg;
    public final double surfaceLength;

    AngleLineParams(
            double angleDeg, double surfaceLength,
            boolean selectNew
    ) {
        super(selectNew);
        this.angleDeg = angleDeg;
        this.surfaceLength = surfaceLength;
    }
}


