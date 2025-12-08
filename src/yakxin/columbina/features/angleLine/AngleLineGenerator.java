package yakxin.columbina.features.angleLine;

import org.openstreetmap.josm.data.osm.Node;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.dto.DrawingNewNodeResult;

public class AngleLineGenerator extends AbstractGenerator<AngleLineParams, Node> {
    @Override
    public DrawingNewNodeResult getNewNodeWayForSingleInput(Node input, AngleLineParams params)  // 内部注意类型检查！
    {
        return null;
    }
}
