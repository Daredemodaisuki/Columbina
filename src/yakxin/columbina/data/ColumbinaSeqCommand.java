package yakxin.columbina.data;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.tools.ImageProvider;

import javax.swing.*;
import java.util.Collection;

public final class ColumbinaSeqCommand extends SequenceCommand {
    // private final String description;
    private final String iconName;

    public ColumbinaSeqCommand(String description, Collection<Command> sequence) {
        this(description, sequence, "Columbina");
    }
    public ColumbinaSeqCommand(String description, Collection<Command> sequence, String iconName) {
        super(description, sequence);
        this.iconName = iconName;
        // this.description = description;
    }

    @Override
    public Icon getDescriptionIcon() {
        return new ImageProvider(iconName).setSize(16,16).get();
    }
}
