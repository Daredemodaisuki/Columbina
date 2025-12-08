package yakxin.columbina.data.dto;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

import java.util.List;
import java.util.Map;

public final class AddCommandsCollected <InputFeatureType extends OsmPrimitive> {
    public final List<Command> commands;
    public final Map<InputFeatureType, Way> inputOutputPairs;
    public final Map<InputFeatureType, List<Long>> failedNodeIds;

    public AddCommandsCollected(List<Command> commands, Map<InputFeatureType, Way> inputOutputPairs, Map<InputFeatureType, List<Long>> failedNodeIds) {
        this.commands = commands;
        this.inputOutputPairs = inputOutputPairs;
        this.failedNodeIds = failedNodeIds;
    }
}


