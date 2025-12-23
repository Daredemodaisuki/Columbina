package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.actionMiddle.ActionWithNodeWay;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsData;

import java.awt.event.KeyEvent;
import java.util.List;

public final class CurveConnectAction extends
        ActionWithNodeWay<CurveConnectGenerator, CurveConnectPreference, CurveConnectParams>
{
    /**
     * 构造函数
     *
     * @param name        功能名称（I18n后）
     * @param iconName    菜单栏功能图标（I18n后）
     * @param description 功能描述（I18n后）
     * @param shortcut    快捷键
     * @param generator   生成器实例
     * @param preference  首选项实例
     */
    public CurveConnectAction(String name, String iconName, String description, Shortcut shortcut, CurveConnectGenerator generator, CurveConnectPreference preference) {
        super(name, iconName, description, shortcut, generator, preference);
    }
    
    /**
     * 懒得在主类填参数就用静态工厂方法
     * @return 构建好的实例
     */
    public static CurveConnectAction create() {
        return new CurveConnectAction(
                I18n.tr("Curve Connect"), "CurveConnect",
                I18n.tr("Connect way ends with curve."),
                Shortcut.registerShortcut(
                        "tools:curveConnect",
                        "More tools: Columbina/Curve Connect",
                        KeyEvent.VK_W,
                        Shortcut.ALT_CTRL_SHIFT
                ),
                new CurveConnectGenerator(),
                new CurveConnectPreference()
        );
    }

    @Override
    public String getUndoRedoInfo(ColumbinaInput inputs, CurveConnectParams params) {
        return "";
    }

    @Override
    public int checkInputNum(ColumbinaInput totalInput) {
        // 检查是否是输入1/2个节点+2条路径，不检查节点是否在路径上（由checkInputDetails判断）
        UtilsData.checkInputNum(totalInput, 1, 2, 2, 2);
        return CHECK_OK;
    }

    @Override
    public int checkInputDetails(List<ColumbinaSingleInput> singleInputs) {
        // 判断节点是否在路径上：
        //   默认先选的路径是startWay，后选的是endWay
        //   允许先选start路径、end节点，再选end线start点，自动匹配：
        //     匹配结果存入快捷传递中间量{"start":[startNode, startNodeIdx], "end":[endNode, endNodeIdx]}
        //   当只选择了1个节点，要求同时在2条路径上
        //   当选择了2个节点，要求至少有1个节点不同时在2条路径上
        //   任何节点都不能是自交路径的自交节点
        //   如果startWay是非闭合的，startNode不能是startWay上第0个节点；如果endWay是非闭合的，endNode不能是endWay上size-1个节点

        // 这个操作不支持批量，前面经过了数量检查，所以只有一组输入
        ColumbinaSingleInput singleInput = singleInputs.get(0);
        Way startWay = singleInput.ways.get(0);
        Way endWay = singleInput.ways.get(1);
        Node node1 = singleInput.nodes.get(0);
        Node node2 = singleInput.nodes.size() == 2 ? singleInput.nodes.get(1) : node1;

        // 计算节点在路径中的索引
        int n1StartIdx = UtilsData.getNodeIndex(node1, startWay);
        int n1EndIdx = UtilsData.getNodeIndex(node1, endWay);
        int n2StartIdx = UtilsData.getNodeIndex(node2, startWay);
        int n2EndIdx = UtilsData.getNodeIndex(node2, endWay);

        // 任何节点都不能是自交路径的自交节点
        if (n1StartIdx == UtilsData.SELF_INTERSECTION || n1EndIdx == UtilsData.SELF_INTERSECTION
                || n2StartIdx == UtilsData.SELF_INTERSECTION || n2EndIdx == UtilsData.SELF_INTERSECTION)
            throw new ColumbinaException(I18n.tr("Self-intersection nodes of self-intersecting ways are not allowed."));

        // 当只选择了1个节点，要求同时在2条路径上
        if (singleInput.nodes.size() == 1) {
            if (n1StartIdx == UtilsData.NODE_NOT_FOUND || n1EndIdx == UtilsData.NODE_NOT_FOUND)
                throw new ColumbinaException(I18n.tr("When only one node is selected, that node must lie on both ways."));
            // 该节点同时作为start和end节点
            singleInput.quickPrecomputedData.put("start", new Object[]{node1, n1StartIdx});
            singleInput.quickPrecomputedData.put("end", new Object[]{node1, n1EndIdx});
        }
        // 当选择了2个节点，要求至少有1个节点不同时在2条路径上
        else {
            boolean node1OnBoth = (n1StartIdx != UtilsData.NODE_NOT_FOUND && n1EndIdx != UtilsData.NODE_NOT_FOUND);
            boolean node2OnBoth = (n2StartIdx != UtilsData.NODE_NOT_FOUND && n2EndIdx != UtilsData.NODE_NOT_FOUND);
            if (node1OnBoth && node2OnBoth)
                throw new ColumbinaException(I18n.tr("When two nodes are selected, at least one node must not be shared by both ways."));
            // 自动匹配start和end节点
            Node startNode, endNode;
            int startIdx, endIdx;
            if (n1StartIdx != UtilsData.NODE_NOT_FOUND && n2EndIdx != UtilsData.NODE_NOT_FOUND) {  // 假定点1为start，点2为end
                startNode = node1; startIdx = n1StartIdx;
                endNode = node2; endIdx = n2EndIdx;
            } else if (n2StartIdx != UtilsData.NODE_NOT_FOUND && n1EndIdx != UtilsData.NODE_NOT_FOUND) {  // 假定点1为end，点2为start
                startNode = node2; startIdx = n2StartIdx;
                endNode = node1; endIdx = n1EndIdx;
            } else
                throw new ColumbinaException(I18n.tr("Cannot find a valid correspondence between the selected nodes with the start and end ways."));
            singleInput.quickPrecomputedData.put("start", new Object[]{startNode, startIdx});
            singleInput.quickPrecomputedData.put("end", new Object[]{endNode, endIdx});
        }

        // 不论选择几个节点，都进行非闭合路径首末点检查
        Object[] startData = (Object[]) singleInput.quickPrecomputedData.get("start");
        Object[] endData = (Object[]) singleInput.quickPrecomputedData.get("end");
        int startIdx = (int) startData[1]; int endIdx = (int) endData[1];
        if (!startWay.isClosed() && startIdx == 0)
            throw new ColumbinaException(I18n.tr("For a non-closed start way, the starting node cannot be the first node."));
        if (!endWay.isClosed() && endIdx == endWay.getNodesCount() - 1)
            throw new ColumbinaException(I18n.tr("For a non-closed end way, the ending node cannot be the last node."));

        return CHECK_OK;
    }
}


