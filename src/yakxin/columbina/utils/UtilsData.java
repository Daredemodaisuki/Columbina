package yakxin.columbina.utils;

import org.openstreetmap.josm.command.*;
import org.openstreetmap.josm.data.osm.*;

import java.util.ArrayList;
import java.util.List;

public class UtilsData {
    /// 数据相关
    public static void wayReplaceNode(Way way, int index, Node newNode) {
        way.removeNode(way.getNode(index));
        way.addNode(index, newNode);
    }

    public static List<Command> tryGetCommandsFromSeqCmd (SequenceCommand seqCmd) {
        List<Command> commands = new ArrayList<>();
        for (PseudoCommand pc : seqCmd.getChildren()) {
            if (pc instanceof Command) {
                commands.add((Command) pc);
            }
        }
        return commands;
    }
}


