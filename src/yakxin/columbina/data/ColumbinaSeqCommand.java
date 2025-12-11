package yakxin.columbina.data;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.tools.ImageProvider;

import javax.swing.*;
import java.util.Collection;

public final class ColumbinaSeqCommand extends SequenceCommand {
    private final String description;
    private final String iconName;

    public ColumbinaSeqCommand(String description, Collection<Command> sequence) {
        this(description, sequence, "Columbina");
    }
    public ColumbinaSeqCommand(String description, Collection<Command> sequence, String iconName) {
        super(description, sequence);
        this.iconName = iconName;
        this.description = description;
    }

    @Override
    public String getDescriptionText() {
        return this.description;  // 不额外添加「序列：」的前缀，也不用额外翻译（调用时翻译了再传入）
    }

    @Override
    public Icon getDescriptionIcon() {
        return new ImageProvider(iconName).setSize(16,16).get();
    }
}


