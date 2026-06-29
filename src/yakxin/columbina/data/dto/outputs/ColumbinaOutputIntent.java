package yakxin.columbina.data.dto.outputs;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.utils.UtilsMath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作意图类
 * <p>由于扩展的功能开始需要变更输入中的原始内容，而这种改变根据情况不同需要不同类型的Command，让generator直接判断、返回Command需要把ds交给它，且逻辑比较杂、明显超出了generator该做的，所以添加这个类
 * <p>意图声明一个希望达到的目标状态或高级操作，一个意图可能在不同情况下对应不同种的一个Command，甚至可能对应多个Command，此乃意图和Command的不同之处
 * <p>这个类的toCommands负责根据意图和数据集现状返回适当的Command列表
 * <p>组内不应有冲突，因为组间的意图之间可能有冲突（如输入组可能会要求移动同一个要素），所以转换时应该汇总全部ColumbinaSingleOutput的intents统一转换，这里会发现冲突并降级为添加节点，这里也会遵循先加节点、再加路径的顺序
 * <p>所有可能存在冲突的意图都需要有降级到不可能冲突的AddCommand和保持不变的fallback
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
        List<Command> deleteCommands = new ArrayList<>();
        
        for (ColumbinaOutputIntent<?> intent : intents) {
            if (intent instanceof AddThisNodeIfOK) {
                AddThisNodeIfOK intentInstance = (AddThisNodeIfOK) intent;
                
                // 正式构建绘制命令
                if (intentInstance.feature != null) {
                    if (!ds.containsNode(intentInstance.feature))  // 新节点在ds中未绘制（不是复用的）才准备绘制
                        addNodeCommands.add(new AddCommand(ds, intentInstance.feature));  // 添加节点到命令序列
                    // 如果已有就不添加，返回空列表
                }
            } else if (intent instanceof AddThisWayIfOK) {
                AddThisWayIfOK intentInstance = (AddThisWayIfOK) intent;
                
                // 正式构建绘制命令
                if (intentInstance.feature != null) {
                    if (!ds.containsWay(intentInstance.feature))  // 新节点在ds中未绘制（不是复用的）才准备绘制
                        addWayCommands.add(new AddCommand(ds, intentInstance.feature));  // 添加节点到命令序列
                    // 如果已有就不添加，返回空列表
                }
            } else if (intent instanceof MergeExistToThisIfOK) {
                MergeExistToThisIfOK intentInstance = (MergeExistToThisIfOK) intent;
                List<OsmPrimitive> existingParents = intentInstance.existingFeature.getReferrers();
                // UtilsUI.testMsgWindow("开始检查合并：\n + new HashSet<>(existingParents) + "\n + new HashSet<>(intentInstance.allowedParents));
                
                assert !ds.containsNode(intentInstance.feature);  // 断言feature还未提交
                
                if (new HashSet<>(existingParents).equals(new HashSet<>(intentInstance.allowedParents))) {
                    // 如果可以移动，则将输入已有的内容移动，并把使用feature的新绘制要素（还未提交）转移到移动后的existingFeature
                    // 在提交之前，新绘制的Way都应该使用新绘制的临时的Node（this.feature）
                    // UtilsUI.testMsgWindow("记录合并对");
                    mergeParis.add(new MergePair(intentInstance.existingFeature, intentInstance.feature, intentInstance.featureParents));
                } else {
                    // 如果计划移动的点不能移动，则保持新绘制要素使用feature，并为feature构建添加指令
                    addNodeCommands.add(new AddCommand(ds, intentInstance.feature));
                }
            } else if (intent instanceof ColumbinaOutputIntent.InsertThisToExistWay) {
                InsertThisToExistWay intentInstance = (InsertThisToExistWay) intent;
                
                int finalIndex = intentInstance.insertIdx;
                if (intentInstance.existingWay.isClosed() && intentInstance.keepClosed && intentInstance.insertIdx == 0)  // 闭合路径用0在闭合点前插需要转换
                    finalIndex = intentInstance.existingWay.getNodesCount() - 1;
                
                List<Node> originalWayNodes = intentInstance.existingWay.getNodes();
                originalWayNodes.add(finalIndex, intentInstance.feature);
                
                if (!ds.containsNode(intentInstance.feature)){
                    addNodeCommands.add(new AddCommand(ds, intentInstance.feature));  // 希望新插入的节点也是要提交的
                }
                insertCommands.add(new ChangeNodesCommand(ds, intentInstance.existingWay, originalWayNodes));
            } else if (intent instanceof DeleteThisNodeIfOK) {
                DeleteThisNodeIfOK intentInstance = (DeleteThisNodeIfOK) intent;
                // 数据集中不存在此节点则不删除
                if (!ds.containsNode(intentInstance.feature)) continue;
                // 检查当前引用者是否与"允许的引用者"一致，确认产生意图后没有新增意外引用
                Set<OsmPrimitive> actualReferrers = new HashSet<>(intentInstance.feature.getReferrers());
                if (actualReferrers.equals(new HashSet<>(intentInstance.allowedReferrers))) {
                    deleteCommands.add(new DeleteCommand(ds, intentInstance.feature));
                }
                // 如果不一致，不删除——可能有遗漏。TODO: 记录失败信息
            }
        }
        
        // 合并对处理
        // 第一遍：检查冲突 + 按 Way 分组记录替换信息
        Map<Node, Integer> pairExistNodeCount = new HashMap<>();
        Map<Way, List<MergePair>> replacementsByWay = new HashMap<>();
        List<MergePair> validPairs = new ArrayList<>();
        
        for (MergePair mergePair : mergeParis) {
            // 计数（检查是否多个intent要求把同一个existingNode合并到不同feature中）
            pairExistNodeCount.put(mergePair.existingNode, pairExistNodeCount.getOrDefault(mergePair.existingNode, 0) + 1);
        }
        for (MergePair mergePair : mergeParis) {
            // 合并没有冲突（没有多个intent要求把同一个existingNode合并到不同feature中）
            if (pairExistNodeCount.get(mergePair.existingNode) == 1) {
                // 无冲突：记录有效合并对，并按 Way 分组
                validPairs.add(mergePair);
                for (OsmPrimitive primitive : mergePair.newNodeParents) {
                    if (primitive instanceof Way)
                        replacementsByWay.computeIfAbsent((Way) primitive, k -> new ArrayList<>()).add(mergePair);
                }
            } else {
                // 有冲突，都不合并，直接添加feature（newNode），也不修改未提交路径
                //  鉴于现在的功能的输入之间不存在共享，按理说输出理应不会产生冲突（没有会共享的输入existingFeature），
                //  应当希望永远不会进入这个逻辑，但保险起见，检查一遍
                //  后面如果需要实现新的意图，也应该考虑与现有要素之间可能的冲突
                addNodeCommands.add(new AddCommand(ds, mergePair.newNode));
            }
        }
        
        // 第二遍：每条Way只做一次批量替换（因为节点变更还未提交，如果每个ChangeNodesCommand读未提交的Way的节点，然后改一个节点，Command之间会互相覆盖）
        // 修改使用未提交的feature节点的未提交路径（未提交的feature不可能被数据集中已有的内容引用，只可能会被新产出、未提交的要素引用）
        // feature.getReferrers查不到未提交节点被哪些未提交路径引用，所以要用Pair中的记录
        for (Map.Entry<Way, List<MergePair>> entry : replacementsByWay.entrySet()) {
            Way way = entry.getKey();
            List<Node> newNodes = new ArrayList<>(way.getNodes());
            for (MergePair mp : entry.getValue()) {
                // 把 Way 节点列表中所有等于 mp.newNode 的引用替换为 mp.existingNode
                for (int i = 0; i < newNodes.size(); i++) {
                    if (newNodes.get(i).getUniqueId() == mp.newNode.getUniqueId())
                        newNodes.set(i, mp.existingNode);
                }
            }
            if (!ds.containsWay(way)) {
                // 引用者还未提交，直接修改内存
                way.setNodes(newNodes);
            } else {
                // 引用者已提交，生成一个整体的 ChangeNodesCommand
                mergeCommands.add(new ChangeNodesCommand(ds, way, newNodes));
            }
        }
        
        // 第三遍：移动指令（每个无冲突的 existingNode 只移动一次）
        for (MergePair mergePair : validPairs) {
            mergeCommands.add(new MoveCommand(mergePair.existingNode, UtilsMath.toLatLon(mergePair.newNode.getEastNorth())));
        }
        
        // 去重并汇总指令（删除放在最后执行，确保其他意图已先解除引用）
        commands.addAll(addNodeCommands.stream().distinct().collect(Collectors.toList()));
        commands.addAll(mergeCommands.stream().distinct().collect(Collectors.toList()));
        commands.addAll(addWayCommands.stream().distinct().collect(Collectors.toList()));
        commands.addAll(insertCommands.stream().distinct().collect(Collectors.toList()));
        commands.addAll(deleteCommands.stream().distinct().collect(Collectors.toList()));
        return commands;
    }
    
    /// 内部类
    private static class MergePair {
        public final Node existingNode;
        public final Node newNode;
        public final List<OsmPrimitive> newNodeParents;
        
        public MergePair(Node existingNode, Node newNode, List<OsmPrimitive> newNodeParents) {
            this.existingNode = existingNode;
            this.newNode = newNode;
            this.newNodeParents = newNodeParents;
        }
    }
    
    /// 具体的意图
    /**
     * 如果数据集中不存在则添加此节点
     */
    public static class AddThisNodeIfOK extends ColumbinaOutputIntent<Node> {
        
        public AddThisNodeIfOK(Node node) {
            super(node, Node.class);
        }
    }
    
    /**
     * 如果数据集中不存在则添加此路径
     */
    public static class AddThisWayIfOK extends ColumbinaOutputIntent<Way> {
        
        public AddThisWayIfOK(Way way) {
            super(way, Way.class);
        }
    }
    
    /**
     * 向已有输入路径添加节点
     */
    public static class InsertThisToExistWay extends ColumbinaOutputIntent<Node> {
        public final Way existingWay;
        public final int insertIdx;
        public final boolean keepClosed;
        
        /**
         * 向已有输入路径添加节点意图的构造函数
         * @param nodeToInsert 希望插入的节点
         * @param existingWay 已有路径
         * @param insertIdx 插点位置（对于闭合路径，为了不改变闭合点，输入0将视作在.size()-1处〔终点前〕插）
         */
        public InsertThisToExistWay(Node nodeToInsert, Way existingWay, int insertIdx) {
            this(nodeToInsert, existingWay, insertIdx, true);
        }
        public InsertThisToExistWay(Node nodeToInsert, Way existingWay, int insertIdx, boolean keepClosed) {
            super(nodeToInsert, Node.class);
            this.existingWay = existingWay;
            this.insertIdx = insertIdx;
            this.keepClosed = keepClosed;
        }
    }
    
    /**
     * 尝试移动输入中的（现有）节点到新位置，并且合并
     */
    public static class MergeExistToThisIfOK extends ColumbinaOutputIntent<Node> {
        public final Node existingFeature;
        public final List<OsmPrimitive> featureParents;  // feature的未提交referrers（通常是输出的新绘制路径），因为feature.getReferrers查不到未提交的引用者，需要手动传入要素
        public final List<OsmPrimitive> allowedParents;  // 如果节点只在allowedParents上（通常是已有的输入），那就可以挪
        
        /**
         * 移动合并意图的构造函数
         * @param existingFeature 希望合并的既存对象（旧位置）
         * @param thisFeature 希望被合并的对象（新位置）
         * @param featureParents thisFeature的未提交referrers：（通常是输出的新绘制路径），因为getReferrers查不到未提交的thisFeature的引用者，需要手动传入要素；如果thisFeature是已提交要素，也需要传入未提交引用者+已提交的getReferrers
         * @param allowedParents 允许existingFeature被什么既有对象引用：（通常是已有的输入），如果节点只在allowedParents上，那就可以挪；只传既有对象，不传新绘制的路径
         */
        public MergeExistToThisIfOK(Node existingFeature, Node thisFeature, List<OsmPrimitive> featureParents, List<OsmPrimitive> allowedParents) {
            super(thisFeature, Node.class);
            this.existingFeature = existingFeature;
            this.featureParents = featureParents;
            this.allowedParents = allowedParents;
        }
    }
    
    /**
     * 如果只有允许的引用者引用它，则删除此节点
     * <p>产生意图时，应已知所有当前引用者，并确认它们将被其他意图解除引用（如通过 ChangeNodesCommand 替换引用）。
     * 转换成 Command 时，先确认数据集中存在此节点，再确认实际引用者与 allowedReferrers 一致，
     * 均通过后才创建 DeleteCommand。
     * 删除应在所有其他类型 Command 之后执行，以确保其他意图已先解除引用。
     * <p>如果不一致（生成意图后有新增意外引用者），则不创建删除指令，确保安全。
     */
    public static class DeleteThisNodeIfOK extends ColumbinaOutputIntent<Node> {
        /** 生成意图时此节点的所有引用者，确认这些引用者将被其他意图解除引用 */
        public final List<OsmPrimitive> allowedReferrers;
        
        /**
         * @param node 要删除的节点
         * @param allowedReferrers 生成意图时此节点的所有引用者（预期其他意图会解除它们的引用）
         */
        public DeleteThisNodeIfOK(Node node, List<OsmPrimitive> allowedReferrers) {
            super(node, Node.class);
            this.allowedReferrers = allowedReferrers;
        }
    }
}


