package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsArc;
import yakxin.columbina.utils.UtilsMath;

public final class CurveConnectGenerator extends AbstractGenerator<CurveConnectParams> {
    // 模式常量，注意这个不是用来判断±1的！
    public static final int COUNTER_CLOCKWISE_MODE = 0;  // 逆时针左拐
    public static final int CLOCKWISE_MODE = 1;  // 顺时针右拐

    @Override
    public ColumbinaSingleOutput getOutputForSingleInput(ColumbinaSingleInput input, CurveConnectParams params) {
        if (input.quickPrecomputedData.get("start") instanceof Object[] && ((Object[]) input.quickPrecomputedData.get("start")).length == 2
                && input.quickPrecomputedData.get("end") instanceof Object[] && ((Object[]) input.quickPrecomputedData.get("end")).length == 2)
        {
            if (((Object[]) input.quickPrecomputedData.get("start"))[0] instanceof Node
                    && ((Object[]) input.quickPrecomputedData.get("start"))[1] instanceof Integer
                    && ((Object[]) input.quickPrecomputedData.get("end"))[0] instanceof Node
                    && ((Object[]) input.quickPrecomputedData.get("end"))[1] instanceof Integer)
            {
                Node startNode = (Node) ((Object[]) input.quickPrecomputedData.get("start"))[0];
                int startIndex = (int) ((Object[]) input.quickPrecomputedData.get("start"))[1];
                Node endNode = (Node) ((Object[]) input.quickPrecomputedData.get("end"))[0];
                int endIndex = (int) ((Object[]) input.quickPrecomputedData.get("end"))[1];
                return buildCorner(
                        startNode, input.ways.get(0), startIndex,
                        endNode, input.ways.get(1), endIndex,
                        params.surfaceCircleRadius, params.surfaceTransArcLength,
                        params.surfaceChainageLength,
                        params.dirMode, true, true
                );
            }
        }
        throw new ColumbinaException(I18n.tr("No or wrong precomputed data received."));
    }

    /**
     * 连接输入的两端，形成圆滑拐角
     * @param startNode 起点
     * @param startWay 起点所在路径
     * @param startNodeIdx 起点索引
     * @param endNode 终点
     * @param endWay 终点所在路径
     * @param endNodeIdx 终点索引
     * @param enCurveRadius 圆曲线半径（R）
     * @param enTransArcLength 缓和曲线长度（ls）
     * @param enChainageLength 桩距（节点间距，米）
     * @param dirMode 模式索引（COUNTER_CLOCKWISE_MODE逆时针左拐〔0〕，CLOCKWISE_MODE顺时针右拐〔1〕）
     * @param ableToAdjustInputNode 对于端头起点和终点，是否允许裁剪
     * @param ableToAdjustCenter 是否允许移动圆心
     * @return 这个拐角缓和曲线包含的点
     */
    private static ColumbinaSingleOutput buildCorner(
            Node startNode, Way startWay, int startNodeIdx,
            Node endNode, Way endWay, int endNodeIdx,
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength,  // 每个桩（节点）之间的距离
            int dirMode, boolean ableToAdjustInputNode, boolean ableToAdjustCenter
    ) {
        /// 计算交点
        //   点startNode(x1, y1) + t·方向向量startDirVec(dx1, dy1) = 点endNode(x2, y2) + s·方向向量endDirVec(dx2, dy2)
        //   x1 + t * dx1 = x2 + s * dx2
        //   y1 + t * dy1 = y2 + s * dy2
        //   解得 t = [(y₂-y₁)·dx₂ - (x₂-x₁)·dy₂] / (dx₂·dy₁ - dx₁·dy₂)
        boolean parallel;
        ColumbinaEN intersect;  // 交点

        int startNodeNum = startWay.isClosed() ? startWay.getNodesCount() - 1 : startWay.getNodesCount();  // 闭合曲线过滤闭合点
        int endNodeNum = endWay.isClosed() ? endWay.getNodesCount() - 1 : endWay.getNodesCount();
        ColumbinaEN start = new ColumbinaEN(startNode.getEastNorth()), end = new ColumbinaEN(endNode.getEastNorth());
        // 入曲线和出曲线方向向量
        ColumbinaEN startDirVec = new ColumbinaEN(startWay.getNode((startNodeIdx + startNodeNum - 1) % startNodeNum), startNode).normVec();
        ColumbinaEN endDirVec = new ColumbinaEN(endNode, endWay.getNode((endNodeIdx + 1) % endNodeNum)).normVec();
        if (endDirVec.east() * startDirVec.north() - startDirVec.east() * endDirVec.north() != 0) {
            double t = ((end.north() - start.north()) * endDirVec.east() - (end.east() - start.east()) * endDirVec.north())
                    / (endDirVec.east() * startDirVec.north() - startDirVec.east() * endDirVec.north());
            parallel = false;
            intersect = start.walk(startDirVec.bearingRad(), t);  // 如果不平行，直接可以计算交点
        } else {
            parallel = true;
            intersect = start.centerBetween(end);  // 如果平行，则直接以start和end中点作为交点
        }

        /// 计算切距切点
        ColumbinaCorner stdCorner;  // 起点在交点前、终点在交点后的标准拐角（不然拐角可能会倒过来）
        UtilsArc.TransArcStartResult transArcStarts;
        if (parallel) {  // 如果是平行的，切距就是切线增长
            double enTangleOffset = UtilsMath.sumSeriesAtVarValue(  // 切线增长（q，或m表示）
                    UtilsArc.getTermTangleOffset(),
                    UtilsArc.TERM_MAX,
                    enTransArcLength,
                    enCurveRadius
            );
            double enShiftDistance = UtilsMath.sumSeriesAtVarValue(  // 内移距离（p）：外圆内圆间距
                    UtilsArc.getTermShiftDistance(),
                    UtilsArc.TERM_MAX,
                    enTransArcLength,
                    enCurveRadius
            );
            // 平行的话没有corner，手动构建起始点、偏角
            //   intersect分别投影到start、end所在线上的点往startDirVec反方向走enTangleOffset距离
            double enStartToProject = new ColumbinaEN(start, intersect).dot(startDirVec);
            double enEndToProject = new ColumbinaEN(end, intersect).dot(endDirVec);
            ColumbinaEN startTanEN = start.walk(startDirVec.bearingRad(), enStartToProject - enTangleOffset);
            ColumbinaEN endTanEN = end.walk(startDirVec.bearingRad(), enEndToProject - enTangleOffset);
            transArcStarts = new UtilsArc.TransArcStartResult(
                    startTanEN, endTanEN,
                    enShiftDistance, enTangleOffset,
                    startDirVec.bearingRad(), UtilsMath.reverseAngleRad(startDirVec.bearingRad()),
                    dirMode == COUNTER_CLOCKWISE_MODE ? UtilsArc.LEFT : UtilsArc.RIGHT
            );
            stdCorner = null;  // 平行的话不适用，放这里占位
        } else {
            // 计算标准拐角（交点沿着起始反方向倒着走10m、沿着结束正方向顺着走10m），保证起点在交点前、终点在交点后
            ColumbinaEN stdStart = intersect.walk(UtilsMath.reverseAngleRad(startDirVec.bearingRad()), 10);
            ColumbinaEN stdEnd = intersect.walk(endDirVec.bearingRad(), 10);
            stdCorner = new ColumbinaCorner(stdStart, intersect, stdEnd);
            // 判断mode和实际转弯方向是否一致以确定是直接弯还是回旋弯
            int directLoop = getDirectLoop(dirMode, stdCorner.leftRight);
            
            transArcStarts = UtilsArc.getStartsOfEulerArcs(stdCorner, directLoop, enCurveRadius, enTransArcLength);
        }

        /// 计算圆心位置
        // 先计算默认圆心
        ColumbinaEN center;
        if (parallel) {
            // 对于平行的，默认圆心就是起点终点之中点（intersect）
            center = intersect;
        } else {
            // 默认从交点向内/外角平分线方向走(圆曲线半径R + 内移距离p) / sin(张角θ / 2)这个长度为圆心
            int leftRight = startDirVec.turnLeftRightTo(endDirVec);
            int directLoop = getDirectLoop(dirMode, leftRight);  // 判断mode和实际转弯方向是否一致以确定是直接弯还是回旋弯
            double enCenterToB = (enCurveRadius + transArcStarts.enShiftDistance) / Math.sin(stdCorner.angleRad / 2);
            // 回旋弯圆心在外角对角线上，直接弯在内角对角线上
            if (directLoop == UtilsArc.LOOP)
                center = stdCorner.B.walk(UtilsMath.reverseAngleRad(stdCorner.getBisectorBearingRad()), enCenterToB);
            else
                center = stdCorner.B.walk(stdCorner.getBisectorBearingRad(), enCenterToB);
        }
        
        // 但还需要判断是否需要移动
        // 依据是切点是否在起点前一个点到起点之间和终点到终点后一个点之间（保证切距OK）
        //  如果在之间，则直接使用切点作为曲线起点，圆心可用不动
        //   如果刚好起点是起点线的最后一个点，或终点是终点线第一个点（两个端头），且用户允许裁切后连接，后续就需要各自裁切
        //   如果是在路径中间，或者前述情况下用户不允许裁切，则单纯添加节点即可，不裁切
        //  如果不在之间：
        //   如果是端头情况，且切点在起点之后、终点之前（相当于只是够不到切点），则沿伸起点、终点所在线段连到切点，圆心也可用不动
        //   如果也不是端头，或者不是「够不到切点」，后续就尝试移动圆心和切点（相当于平移曲线），如果用户也不允许调整圆心或无法调整，则失败
        if (ifCanUseTanNode(startNode, startWay, startNodeIdx, endNode, endWay, endNodeIdx, transArcStarts)) {  // 切点在之间
            // TODO
        } else {  // 切点不在之间
            // TODO
            //  调整圆心：
            //   如果是直接弯，无法调整
            //   如果是回旋弯，起点或（和）终点缺多少切距，就按照对应方向分别移动相应的差的切距，最终刚好输入的start或（和）end就是切点
            //   （如果是一边差一边不差，只修改差的那边）
        }

        /// 绘制
        return null;
    }
    
    /**
     * 判断mode和实际转弯方向是否一致以确定是直接弯还是回旋弯
     * @param dirMode 顺时针还是逆时针模式
     * @param leftRight 拐角方向
     * @return 直接弯还是回旋弯
     */
    private static int getDirectLoop(int dirMode, int leftRight) {
        int directLoop;
        if ((leftRight == UtilsArc.LEFT && dirMode == CurveConnectGenerator.COUNTER_CLOCKWISE_MODE)
                || (leftRight == UtilsArc.RIGHT && dirMode == CurveConnectGenerator.CLOCKWISE_MODE))
            directLoop = UtilsArc.DIRECT;
        else if ((leftRight == UtilsArc.RIGHT && dirMode == CurveConnectGenerator.COUNTER_CLOCKWISE_MODE)
                || (leftRight == UtilsArc.LEFT && dirMode == CurveConnectGenerator.CLOCKWISE_MODE))
            directLoop = UtilsArc.LOOP;
        else
            throw new ColumbinaException("getDirectLoop: Unexpected match between dirMode and corner.leftRight.");
        return directLoop;
    }
    
    /**
     * 根据原起始、结束路径和起始点、终点以及切点数据判断是否可以直接使用切点数据给出的起点、终点
     * <p>起始就是检查起始端切点是否在起始路径上原起点前一个点和原起点之间，同理，检查检查结束端切点是否在结束路径上原终点和原终点后一个点之间
     * <p>如果在，则说明切点在起始路径和结束路径上，可以直接使用
     * <p>如果不在，则说明切点在路径以外的地方，不能使用，下一步应尝试移动圆心
     * @param startNode 原起点
     * @param startWay 起始路径
     * @param startNodeIdx 原起点在起始路径上的索引
     * @param endNode 原终点
     * @param endWay 终点路径
     * @param endNodeIdx 原终点在终点路径上的索引
     * @param transArcStarts 切点数据
     * @return 是否可以直接使用切点
     */
    private static boolean ifCanUseTanNode(
            Node startNode, Way startWay, int startNodeIdx,
            Node endNode, Way endWay, int endNodeIdx,
            UtilsArc.TransArcStartResult transArcStarts
    ) {
        int startNodeNum = startWay.isClosed() ? startWay.getNodesCount() - 1 : startWay.getNodesCount();  // 闭合曲线过滤闭合点
        int endNodeNum = endWay.isClosed() ? endWay.getNodesCount() - 1 : endWay.getNodesCount();
        
        ColumbinaEN beforeStart = new ColumbinaEN(startWay.getNode((startNodeIdx + startNodeNum - 1) % startNodeNum).getEastNorth());
        ColumbinaEN startTan = new ColumbinaEN(transArcStarts.startA);
        ColumbinaEN start = new ColumbinaEN(startNode.getEastNorth());
        ColumbinaEN end = new ColumbinaEN(endNode.getEastNorth());
        ColumbinaEN endTan = new ColumbinaEN(transArcStarts.startC);
        ColumbinaEN afterEnd = new ColumbinaEN(endWay.getNode((endNodeIdx + 1) % endNodeNum).getEastNorth());
        
        return ColumbinaEN.isBOnAC(beforeStart, startTan, start) && ColumbinaEN.isBOnAC(end, endTan, afterEnd);
    }
    
    private static void adjustCircleCenter() {
    
    }
}


