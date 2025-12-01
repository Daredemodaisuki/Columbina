package yakxin.columbina.data;

import org.openstreetmap.josm.data.osm.Node;

import java.util.List;

public final class FilletResult {
    public List<Node> newNodes;
    public List<Long> failedNodes;

    public FilletResult(List<Node> nN, List<Long> fN) {
        newNodes = nN;
        failedNodes = fN;
    }
}


