package yakxin.columbina.data.dto;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;
import java.util.Map;

public final class AddCommandsCollected {
    public final List<Command> commands;
    public final Map<Way, Way> oldNewWayPairs;
    public final Map<Way, List<Long>> failedNodeIds;

    public AddCommandsCollected(List<Command> commands, Map<Way, Way> oldNewWayPairs, Map<Way, List<Long>> failedNodeIds) {
        this.commands = commands;
        this.oldNewWayPairs = oldNewWayPairs;
        this.failedNodeIds = failedNodeIds;
    }
}
