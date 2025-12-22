package yakxin.columbina.data.dto.outputs;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.utils.UtilsData;
import yakxin.columbina.utils.UtilsMath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作意图类
 * <p>由于扩展的功能开始需要变更输入中的原始内容，而这种改变根据情况不同需要不同类型的Command，让generator直接判断、返回Command需要把ds交给它，且逻辑比较杂、明显超出了generator该做的，所以添加这个类
 * <p>意图声明一个希望达到的目标状态或高级操作，一个意图可能在不同情况下对应不同种的一个Command，甚至可能对应多个Command，此乃意图和Command的不同之处
 * <p>这个类的toCommands负责根据意图和数据集现状返回适当的Command列表
 * <p>因为不同意图之间可能由冲突（如输入组可能会要求移动同一个要素），所以转换时应该汇总全部ColumbinaSingleOutput的intents统一转换，这里会发现冲突并降级为添加节点，这里也会遵循先加节点、再加路径的顺序
 */
public abstract class ColumbinaOutputIntent <T extends OsmPrimitive> {
    public final T feature;
    public final Class<T> featureType;
    
    public ColumbinaOutputIntent(T feature, Class<T> featureType) {
        this.feature = feature;
        this.featureType = featureType;
    }
    
    /**
     * 意图协调解释器
     * <p>负责将意图转为Command列表，协调可能的意图冲突，并在提交前Command修改内存中的要素实体（如果必要）
     * <p>应该遵循以下流程：<ul>
     *     <li>先添加新节点（AddThisNodeIfOK）</li>
     *     <li>尝试合并输入中的（现有）节点到新位置（MergeExistToThisIfOK）之上半：如果节点被其他引用，则改作新加节点；否则记录合并对</li>
     *     <li>向输入路径添加节点</li>
     *     <li>尝试合并输入中的（现有）节点到新位置（MergeExistToThisIfOK）之下半：根据合并对检查有无意图冲突（同时请求合并一个点到不同位置），如果没有就移动节点，并修改内存中的引用；如果有就降级为添加节点</li>
     *     <li>添加路径（AddThisWayIfOK）</li>
     * </ul>
     * <p>目前还没有批量需要「尝试移动输入中的（现有）节点到新位置」的功能，有了之后应该测试
     * @param intents 所有ColumbinaSingleOutput产生的意图汇总列表
     * @return 指令列表
     */
    public static List<Command> toCommands(List<ColumbinaOutputIntent<?>> intents, DataSet ds) {
        List<Command> commands = new ArrayList<>();
        List<Command> addNodeCommands = new ArrayList<>();
        List<MergePair> mergeParis = new ArrayList<>();  // 合并对
        List<Command> mergeCommands = new ArrayList<>();
        List<Command> addWayCommands = new ArrayList<>();
        List<Command> insertCommands = new ArrayList<>();
        
        for (ColumbinaOutputIntent<?> intent : intents) {
            if (intent instanceof AddThisNodeIfOK) {
                AddThisNodeIfOK intentInstance = (AddThisNodeIfOK) intent;
                // 设置标签
                if (!intentInstance.tags.isEmpty())
                    intentInstance.feature.setKeys(intentInstance.tags);
                
                // 正式构建绘制命令
                if (intentInstance.feature != null) {
                    if (!ds.containsNode(intentInstance.feature))  // 新节点在ds中未绘制（不是复用的）才准备绘制
                        addNodeCommands.add(new AddCommand(ds, intentInstance.feature));  // 添加节点到命令序列
                    // 如果已有就不添加，返回空列表
                }
            } else if (intent instanceof AddThisWayIfOK) {
                AddThisWayIfOK intentInstance = (AddThisWayIfOK) intent;
                // 设置标签
                if (!intentInstance.tags.isEmpty())
                    intentInstance.feature.setKeys(intentInstance.tags);
                
                // 正式构建绘制命令
                if (intentInstance.feature != null) {
                    if (!ds.containsWay(intentInstance.feature))  // 新节点在ds中未绘制（不是复用的）才准备绘制
                        addWayCommands.add(new AddCommand(ds, intentInstance.feature));  // 添加节点到命令序列
                    // 如果已有就不添加，返回空列表
                }
            } else if (intent instanceof MergeExistToThisIfOK) {
                MergeExistToThisIfOK intentInstance = (MergeExistToThisIfOK) intent;
                List<OsmPrimitive> existingParents = intentInstance.existingFeature.getReferrers();
                // UtilsUI.testMsgWindow("开始检查合并：\n"
                //         + new HashSet<>(existingParents) + "\n"
                //         + new HashSet<>(intentInstance.allowedParents)
                // );
                
                assert !ds.containsNode(intentInstance.feature);
                if (new HashSet<>(existingParents).equals(new HashSet<>(intentInstance.allowedParents))) {
                    // 如果可以移动，则将输入已有的内容移动，并把使用feature的新绘制要素（还未提交）转移到移动后的existingFeature
                    // 在提交之前，新绘制的Way都应该使用新绘制的临时的Node（this.feature）
                    // UtilsUI.testMsgWindow("记录合并对");
                    mergeParis.add(new MergePair(intentInstance.existingFeature, intentInstance.feature, intentInstance.featureParents));
                } else {
                    // 如果计划移动的点不能移动，则保持新绘制要素使用feature，并为feature构建添加指令
                    addNodeCommands.add(new AddCommand(ds, intentInstance.feature));
                }
            } else if (intent instanceof InsertThisToWay) {
                InsertThisToWay intentInstance = (InsertThisToWay) intent;
                
                List<Node> originalWayNodes = intentInstance.existingWay.getNodes();
                originalWayNodes.add(intentInstance.insertIdx, intentInstance.feature);
                
                addNodeCommands.add(new AddCommand(ds, intentInstance.feature));  // 希望新插入的节点也是要提交的
                insertCommands.add(new ChangeNodesCommand(ds, intentInstance.existingWay, originalWayNodes));
            }
        }
        
        // 合并对处理
        Map<Node, Integer> pairExistNodeCount = new HashMap<>();
        for (MergePair mergePair : mergeParis)  // 计数
            pairExistNodeCount.put(mergePair.existingNode, pairExistNodeCount.getOrDefault(mergePair.existingNode, 0) + 1);
        for (MergePair mergePair : mergeParis) {
            // 合并没有冲突（没有多个intent要求把同一个existingNode合并到不同feature中）
            if (pairExistNodeCount.get(mergePair.existingNode) == 1) {
                mergeCommands.add(new MoveCommand(mergePair.existingNode, UtilsMath.toLatLon(mergePair.tempNewNode.getEastNorth())));
                // 修改使用未提交的feature节点的未提交路径（未提交的feature不可能被数据集中已有的内容引用，只可能会被新产出、未提交的要素引用）
                // feature.getReferrers查不到未提交节点被哪些未提交路径引用，所以要用Pair中的记录
                for (OsmPrimitive primitive : mergePair.tempNewNodeParents) {
                    // UtilsUI.testMsgWindow("正在修改" + primitive);
                    if (primitive instanceof Way) {
                        assert !ds.containsWay((Way) primitive);  // 断言引用者还未提交
                        int index = 0;
                        for (Node n : ((Way) primitive).getNodes()) {
                            if (n.getUniqueId() == mergePair.tempNewNode.getUniqueId())
                                UtilsData.wayReplaceNode((Way) primitive, index, mergePair.existingNode);
                            index++;
                        }
                    }
                }
            } else {
                // 有冲突，都不合并，直接添加feature（tempNewNode），也不修改未提交路径
                addNodeCommands.add(new AddCommand(ds, mergePair.tempNewNode));
            }
        }
        
        // 去重并汇总指令
        commands.addAll(addNodeCommands.stream().distinct().collect(Collectors.toList()));
        commands.addAll(mergeCommands.stream().distinct().collect(Collectors.toList()));
        commands.addAll(addWayCommands.stream().distinct().collect(Collectors.toList()));
        commands.addAll(insertCommands.stream().distinct().collect(Collectors.toList()));
        return commands;
    }
    
    /// 内部类
    private static class MergePair {
        public final Node existingNode;
        public final Node tempNewNode;
        public final List<OsmPrimitive> tempNewNodeParents;
        
        public MergePair(Node existingNode, Node tempNewNode, List<OsmPrimitive> tempNewNodeParents) {
            this.existingNode = existingNode;
            this.tempNewNode = tempNewNode;
            this.tempNewNodeParents = tempNewNodeParents;
        }
    }
    
    /// 具体的意图
    /**
     * 如果数据集中不存在则添加此节点
     */
    public static class AddThisNodeIfOK extends ColumbinaOutputIntent<Node> {
        private Map<String, String> tags;
        
        public AddThisNodeIfOK(Node node) {
            super(node, Node.class);
            this.tags = new HashMap<>();
        }
        public AddThisNodeIfOK(Node node, Map<String, String> tags) {
            super(node, Node.class);
            this.tags = tags;
        }
        
        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }
    
    /**
     * 如果数据集中不存在则添加此路径
     */
    public static class AddThisWayIfOK extends ColumbinaOutputIntent<Way> {
        private Map<String, String> tags;
        
        public AddThisWayIfOK(Way way) {
            super(way, Way.class);
            this.tags = new HashMap<>();
        }
        public AddThisWayIfOK(Way way, Map<String, String> tags) {
            super(way, Way.class);
            this.tags = tags;
        }
        
        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }
    
    /**
     * 向输入路径添加节点
     */
    public static class InsertThisToWay extends ColumbinaOutputIntent<Node> {
        public final Way existingWay;
        public final int insertIdx;
        
        public InsertThisToWay(Node feature, Way existingWay, int insertIdx) {
            super(feature, Node.class);
            this.existingWay = existingWay;
            this.insertIdx = insertIdx;
        }
    }
    
    /**
     * 尝试移动输入中的（现有）节点到新位置，并且合并
     */
    public static class MergeExistToThisIfOK extends ColumbinaOutputIntent<Node> {
        public final Node existingFeature;
        public final List<OsmPrimitive> featureParents;  // 未提交的feature的未提交parents（通常是输出的路径），因为feature.getReferrers查不到被些未提交路径引用
        public final List<OsmPrimitive> allowedParents;  // 如果节点只在allowedParents上（通常是已有的输入），那就可以挪
        
        public MergeExistToThisIfOK(Node existingFeature, Node thisFeature) {
            this(existingFeature, thisFeature, new ArrayList<>(), new ArrayList<>());
        }
        public MergeExistToThisIfOK(Node existingFeature, Node thisFeature, List<OsmPrimitive> featureParents, List<OsmPrimitive> allowedParents) {
            super(thisFeature, Node.class);
            this.existingFeature = existingFeature;
            this.featureParents = featureParents;
            this.allowedParents = allowedParents;
        }
    }
}


