package yakxin.columbina.data.dto;

import org.openstreetmap.josm.data.osm.Node;

import java.util.List;

public final class DrawingNewNodeResult {
    public List<Node> newNodes;
    public List<Long> failedNodes;

    public DrawingNewNodeResult(List<Node> nN, List<Long> fN) {
        newNodes = nN;
        failedNodes = fN;
    }
}


