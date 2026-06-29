package yakxin.columbina.features.mergeUnusedNodes;

import yakxin.columbina.abstractClasses.AbstractParams;

public class MergeUnusedNodesParams extends AbstractParams {
    MergeUnusedNodesParams() {
        super();
        this.copyTag = false;
        this.selectNew = true;
        this.deleteOld = false;
    }
}