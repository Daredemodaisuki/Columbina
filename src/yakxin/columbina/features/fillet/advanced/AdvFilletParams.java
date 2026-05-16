package yakxin.columbina.features.fillet.advanced;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdvFilletParams {
    // 按路径ID映射的各拐角表面半径列表，键为路径ID，值为该路径上各拐角的半径
    public final Map<Long, List<Double>> surfaceRadiusListByWayId;

    public AdvFilletParams(Map<Long, List<Double>> surfaceRadiusListByWayId) {
        this.surfaceRadiusListByWayId = Collections.unmodifiableMap(new LinkedHashMap<>(surfaceRadiusListByWayId));
    }

    /**
     * 获取指定路径的拐角半径列表
     * @param wayId 路径ID
     * @return 该路径上各拐角的半径列表，若不存在则返回null
     */
    public List<Double> getSurfaceRadiusList(long wayId) {
        return surfaceRadiusListByWayId.get(wayId);
    }

    /**
     * 判断是否为空（没有任何路径的拐角半径数据）
     * @return 是否为空
     */
    public boolean isEmpty() {
        return surfaceRadiusListByWayId.isEmpty();
    }
}
