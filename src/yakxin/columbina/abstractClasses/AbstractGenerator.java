package yakxin.columbina.abstractClasses;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import yakxin.columbina.data.dto.DrawingNewNodeResult;

public abstract class AbstractGenerator <ParamType extends AbstractParams, InputFeatureType extends OsmPrimitive> {
    /// 具体类必须实现的
    public abstract DrawingNewNodeResult getNewNodeWayForSingleInput(InputFeatureType input, ParamType params);  // 内部注意类型检查！

    public AbstractGenerator() {}
}
