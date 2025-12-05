package yakxin.columbina.data.dto;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;

public final class NewNodeWayCommands {
    public final Way newWay;
    public final List<Command> addCommands;
    public final List<Long> failedNodeIds;

    public NewNodeWayCommands(Way newWay, List<Command> addCommands, List<Long> failedNodeIds) {
        this.newWay = newWay;
        this.addCommands = addCommands;
        this.failedNodeIds = failedNodeIds;
    }
}


