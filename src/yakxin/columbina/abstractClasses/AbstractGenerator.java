package yakxin.columbina.abstractClasses;

import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.data.dto.DrawingNewNodeResult;

public abstract class AbstractGenerator <ParamType extends AbstractParams> {
    /// 具体类必须实现的
    public abstract DrawingNewNodeResult getNewNodeWay(Way way, ParamType params);  // 内部注意类型检查！

    public AbstractGenerator() {}
}
