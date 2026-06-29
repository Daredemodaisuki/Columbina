package yakxin.columbina.features.mergeUnusedNodes;

import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractPreference;
import yakxin.columbina.data.ColumbinaPrefItem;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

import javax.swing.*;

public class MergeUnusedNodesPreference extends AbstractPreference<MergeUnusedNodesParams> {
    protected MergeUnusedNodesPreference() {
        super(new ColumbinaPrefItem[] {});
    }

    @Override
    public MergeUnusedNodesParams getParamsAndUpdatePreference(ColumbinaInput input) {
        int confirm = JOptionPane.showConfirmDialog(
                null,
                I18n.tr("Are you sure to merge existing unused nodes to new nodes? Please make sure you have downloaded all references for existing nodes."),
                I18n.tr("Columbina"),
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.NO_OPTION) return null;
        else return new MergeUnusedNodesParams();
    }
}