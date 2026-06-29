package yakxin.columbina.features.mergeUnusedNodes;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.data.dto.outputs.ColumbinaOutputIntent;
import yakxin.columbina.data.dto.outputs.ColumbinaSingleOutput;
import yakxin.columbina.utils.UtilsData;

import java.util.*;

public class MergeUnusedNodesGenerator extends AbstractGenerator<MergeUnusedNodesParams> {
    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, MergeUnusedNodesParams params) {
        DataSet ds = UtilsData.getEditDataSet();
        if (ds == null) return null;

        // 分类节点：已上传但未使用的节点 vs 新绘制的未上传节点
        List<Node> existingUnusedNodes = new ArrayList<>();
        List<Node> newUploadableNodes = new ArrayList<>();

        for (Node node : ds.getNodes()) {
            if (node.getUniqueId() > 0) {
                // 已上传的节点：检查是否未使用（无标签、无引用者）
                List<OsmPrimitive> referrers = node.getReferrers();
                boolean referenced = (referrers != null && !referrers.isEmpty()) || node.hasKeys();
                if (!referenced)
                    existingUnusedNodes.add(node);
            } else if (node.getUniqueId() < 0) {
                // 新绘制的未上传节点：必须有引用者或有标签才纳入合并（否则是无意义的新节点）
                List<OsmPrimitive> referrers = node.getReferrers();
                boolean isUseful = (referrers != null && !referrers.isEmpty()) || node.hasKeys();
                if (isUseful)
                    newUploadableNodes.add(node);
            }
        }

        if (existingUnusedNodes.isEmpty()) {
            return new ColumbinaSingleOutput(I18n.tr("No unused existing nodes found in the dataset."));
        }
        if (newUploadableNodes.isEmpty()) {
            return new ColumbinaSingleOutput(I18n.tr("No new un-uploaded nodes found in the dataset."));
        }

        // 配对合并：每个未使用的已上传节点 ← 一个新绘制的节点
        List<ColumbinaOutputIntent<?>> intents = new ArrayList<>();
        List<OsmPrimitive> representatives = new ArrayList<>();
        Map<OsmPrimitive, String> failedNodes = new HashMap<>();

        int pairCount = Math.min(existingUnusedNodes.size(), newUploadableNodes.size());
        for (int i = 0; i < pairCount; i++) {
            Node existingNode = existingUnusedNodes.get(i);
            Node newNode = newUploadableNodes.get(i);

            // 预拷贝标签：将新节点的标签复制到已有节点上（在创建命令之前修改内存）
            if (newNode.hasKeys()) {
                existingNode.setKeys(newNode.getKeys());
            }

            List<OsmPrimitive> newNodeReferrers = newNode.getReferrers();

            // 创建合并意图：existingNode（已有未使用）← newNode（新绘制的目标位置）
            intents.add(new ColumbinaOutputIntent.MergeExistToThisIfOK(
                    existingNode, newNode,
                    newNodeReferrers,
                    Collections.emptyList()
            ));

            // 创建删除意图：合并后 newNode 的引用已被 ChangeNodesCommand 解除，可删除
            intents.add(new ColumbinaOutputIntent.DeleteThisNodeIfOK(
                    newNode,
                    newNodeReferrers
            ));

            representatives.add(existingNode);
        }

        // 如果有更多未使用的已有节点，报部分失败
        if (pairCount < existingUnusedNodes.size()) {
            for (int i = pairCount; i < existingUnusedNodes.size(); i++) {
                failedNodes.put(existingUnusedNodes.get(i),
                        I18n.tr("No corresponding new node found for existing unused node {0}.",
                                existingUnusedNodes.get(i).getUniqueId()));
            }
        }

        return new ColumbinaSingleOutput(intents, representatives, failedNodes.isEmpty() ? null : failedNodes, new HashMap<>());
    }
}