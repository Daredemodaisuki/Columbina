package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import yakxin.columbina.abstractClasses.AbstractGenerator;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.outputs.ColumbinaOutputIntent;
import yakxin.columbina.data.dto.outputs.ColumbinaSingleOutput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;
import yakxin.columbina.utils.UtilsArc;
import yakxin.columbina.utils.UtilsMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class CurveConnectGenerator extends AbstractGenerator<CurveConnectParams> {
    // 模式常量，注意这个不是用来判断±1的！
    public static final int COUNTER_CLOCKWISE_MODE = 0;  // 逆时针左拐
    public static final int CLOCKWISE_MODE = 1;  // 顺时针右拐
    // 切点位置
    // public static final int START_TAN_ON_BS_S = 0;  // 起点端切点位于beforeStart和start之间（默认情况）
    // public static final int START_TAN_ON_S_AS = 1;  // 起点端切点位于start和afterStart之间（默认失败时尝试）
    // public static final int END_TAN_ON_E_AE = 2;  // 终点端切点位于end和afterEnd之间（默认情况）
    // public static final int END_TAN_ON_BE_E = 3;  // 终点端切点位于beforeEnd和end之间（默认失败时尝试）
    // 切点策略
    public static final int FAILED = -1;
    public static final int ADJUST_EXISTING_INPUT_WAY_END_NODES = 0;
    public static final int ADD_NODES = 1;

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
                        params.dirMode, params.ableToAdjustInputNode
                );
            }
        }
        throw new ColumbinaException("CurveConnectGenerator.getOutputForSingleInput: No or wrong precomputed data received.");
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
     * @param ableToAdjustInputNode 对于端头起点和终点，是否允许裁剪或沿伸
     * @return 这个拐角缓和曲线包含的点和其他信息
     */
    private static ColumbinaSingleOutput buildCorner(
            Node startNode, Way startWay, int startNodeIdx,
            Node endNode, Way endWay, int endNodeIdx,
            double enCurveRadius, double enTransArcLength,  // 圆曲线半径（内圆R）、缓和段长度（ls）
            double enChainageLength,  // 每个桩（节点）之间的距离
            int dirMode, boolean ableToAdjustInputNode
    ) {
        /// 计算交点
        //   点startNode(x1, y1) + t·方向向量startDirVec(dx1, dy1) = 点endNode(x2, y2) + s·方向向量endDirVec(dx2, dy2)
        //   x1 + t * dx1 = x2 + s * dx2
        //   y1 + t * dy1 = y2 + s * dy2
        //   解得 t = [(y₂-y₁)·dx₂ - (x₂-x₁)·dy₂] / (dx₂·dy₁ - dx₁·dy₂)
        final int startNodeNum = startWay.isClosed() ? startWay.getNodesCount() - 1 : startWay.getNodesCount();  // 闭合曲线过滤闭合点
        final int endNodeNum = endWay.isClosed() ? endWay.getNodesCount() - 1 : endWay.getNodesCount();
        ColumbinaEN start = new ColumbinaEN(startNode.getEastNorth()), end = new ColumbinaEN(endNode.getEastNorth());
        // 入曲线和出曲线方向向量
        Node beforeStartNode = startWay.getNode((startNodeIdx + startNodeNum - 1) % startNodeNum);
        Node afterEndNode = endWay.getNode((endNodeIdx + 1) % endNodeNum);
        ColumbinaEN startDirVec = new ColumbinaEN(beforeStartNode, startNode).normVec();
        ColumbinaEN endDirVec = new ColumbinaEN(endNode, afterEndNode).normVec();
        
        IntersectResult intersectResult = getIntersectResult(start, startDirVec, end, endDirVec); // 交点和是否平行
        
        /// 计算切距切点
        ColumbinaCorner stdCorner;  // 起点在交点前、终点在交点后的标准拐角（不然拐角可能会倒过来）
        UtilsArc.TransArcStartResult transArcStarts;
        if (intersectResult.parallel) {  // 如果是平行的，则无法产生
            return null;
        } else {
            // 计算标准拐角（交点沿着起始反方向倒着走10m、沿着结束正方向顺着走10m），保证起点在交点前、终点在交点后
            ColumbinaEN stdStart = intersectResult.intersect.walk(UtilsMath.reverseAngleRad(startDirVec.bearingRad()), 10);
            ColumbinaEN stdEnd = intersectResult.intersect.walk(endDirVec.bearingRad(), 10);
            stdCorner = new ColumbinaCorner(stdStart, intersectResult.intersect, stdEnd);
            // 判断mode和实际转弯方向是否一致以确定是直接弯还是回旋弯
            int directLoop = getDirectLoop(dirMode, stdCorner.leftRight);
            
            transArcStarts = UtilsArc.getStartsOfEulerArcs(stdCorner, directLoop, enCurveRadius, enTransArcLength);
        }

        /// 计算圆心位置
        ColumbinaEN center;
        // 默认从交点向内/外角平分线方向走(圆曲线半径R + 内移距离p) / sin(张角θ / 2)这个长度为圆心
        int leftRight = startDirVec.turnLeftRightTo(endDirVec);
        int directLoop = getDirectLoop(dirMode, leftRight);  // 判断mode和实际转弯方向是否一致以确定是直接弯还是回旋弯
        double enCenterToB = (enCurveRadius + transArcStarts.enShiftDistance) / Math.sin(stdCorner.angleRad / 2);
        // 回旋弯圆心在外角对角线上，直接弯在内角对角线上
        if (directLoop == UtilsArc.LOOP)
            center = stdCorner.B.walk(UtilsMath.reverseAngleRad(stdCorner.getBisectorBearingRad()), enCenterToB);
        else
            center = stdCorner.B.walk(stdCorner.getBisectorBearingRad(), enCenterToB);
        
        /// 判断切距是否足够以确定是否需要裁切或沿伸
        // 是否是端头
        boolean isStartLastNode = !startWay.isClosed() && startNodeIdx == startNodeNum - 1;
        boolean isEndFirstNode = !endWay.isClosed() && endNodeIdx == 0;
        ColumbinaEN beforeStart = new ColumbinaEN(beforeStartNode.getEastNorth()), startTan = new ColumbinaEN(transArcStarts.startA);
        ColumbinaEN afterEnd = new ColumbinaEN(afterEndNode.getEastNorth()), endTan = new ColumbinaEN(transArcStarts.startC);
        // 匹配切点策略
        TanNodeStrategyResult tanNodeStrategy = getTanNodeStrategyResult(
                ableToAdjustInputNode, startNode.getUniqueId() != endNode.getUniqueId(),
                beforeStart, startTan, start, isStartLastNode,
                end, endTan, afterEnd, isEndFirstNode
        );
        // TODO：return换成throw ex
        if (tanNodeStrategy.startMethod == FAILED || tanNodeStrategy.endMethod == FAILED) return null;
        
        /// 绘制螺旋线
        int actualLeftRight = dirMode == COUNTER_CLOCKWISE_MODE ? UtilsArc.LEFT : UtilsArc.RIGHT;
        // TODO：缓和曲线长度太长导致回旋线部分的转角就大于了总偏转角，导致曲线直接绕了一圈
        // A侧螺旋线（从A侧直缓切点顺着画）
        UtilsArc.SingleEulerArcResult unrotatedTransArcA = UtilsArc.getUnrotatedEulerArc(  // 绘制
                enCurveRadius, enTransArcLength,
                enChainageLength,
                actualLeftRight  // 因为这里定义了顺逆时针，不用transArcStarts里的左右
        );
        
        UtilsArc.SingleEulerArcResult rotatedTransArcA = UtilsArc.rotateAndMoveEulerArc(  // 旋转、移动
                transArcStarts.startA,
                transArcStarts.startAngleARad,
                unrotatedTransArcA
        );
        // C侧螺旋线（从C侧直缓切点开始倒着画）
        UtilsArc.SingleEulerArcResult unrotatedTransArcC = UtilsArc.getUnrotatedEulerArc(  // 绘制
                enCurveRadius, enTransArcLength,
                enChainageLength,
                -actualLeftRight  // C侧是倒回来画的，与A到C方向的左右相反
        );
        UtilsArc.SingleEulerArcResult rotatedTransArcC = UtilsArc.rotateAndMoveEulerArc(  // 旋转、移动
                transArcStarts.startC,
                transArcStarts.startAngleCRad,
                unrotatedTransArcC
        );
        
        /// 圆曲线
        // 计算段数（圆心角）
        double tangentBearingARad = rotatedTransArcA.endTangentAngleRad;
        double tangentBearingCRad = UtilsMath.normAngleRad(rotatedTransArcC.endTangentAngleRad + Math.PI);  // C侧双螺旋是倒过来画的，所以它绘制意义上的（终点）出曲线方向取反向才是行进方向A→B→C的角度
        double centralAngleRad = UtilsMath.normAngleRad(tangentBearingCRad - tangentBearingARad);  // 防止AB、BC跨±180°线时画优弧（「<」这种情况）
        int numAngleSteps = Math.abs((int) (enCurveRadius * centralAngleRad / enChainageLength));
        // 画曲线
        List<EastNorth> circularArc = UtilsArc.getCircleArc(
                center, enCurveRadius,
                tangentBearingARad, tangentBearingCRad,
                numAngleSteps, actualLeftRight
        );
        
        /// 拼接
        // 整理用于拼接的点
        List<EastNorth> transArcA = new ArrayList<>(rotatedTransArcA.arcNodes);
        List<EastNorth> transArcC = new ArrayList<>(rotatedTransArcC.arcNodes);
        if (transArcA.size() < 2 || circularArc.size() < 2 || transArcC.size() < 2) return null;  // 曲线不完整，绘制失败
        transArcA.remove(transArcA.size() - 1);  // 不要ArcA的最后一个点（=圆曲线第一个点）
        Collections.reverse(transArcC);  // 倒着画的原地逆序正回来
        transArcC.remove(0);  // 正序之后不要第一个点（=圆曲线最后一个点）
        
        // 最终节点列表
        ArrayList<EastNorth> finalNodeEN = new ArrayList<>();
        finalNodeEN.addAll(transArcA);
        finalNodeEN.addAll(circularArc);
        finalNodeEN.addAll(transArcC);
        ArrayList<Node> finalNodes = new ArrayList<>();
        for (EastNorth en : finalNodeEN)
            finalNodes.add(new Node(en));
        
        // 画线
        Way finalWay = new Way();
        for (Node n : finalNodes) finalWay.addNode(n);
        
        // 添加为意图
        List<ColumbinaOutputIntent<?>> intents = new ArrayList<>();
        for (int i = 0; i < finalNodeEN.size(); i ++) {
            if (i == 0) {
                if (tanNodeStrategy.startMethod == ADJUST_EXISTING_INPUT_WAY_END_NODES) {
                    intents.add(new ColumbinaOutputIntent.MergeExistToThisIfOK(startNode, finalNodes.get(0), List.of(finalWay), List.of(startWay)));
                    // UtilsUI.testMsgWindow("i == 0");
                }
                else  // 原startNodeIdx前插，插完后新点为startNodeIdx
                    intents.add(new ColumbinaOutputIntent.InsertThisToExistWay(finalNodes.get(0), startWay, startNodeIdx));
            }
            else if (i == finalNodes.size() - 1) {
                if (tanNodeStrategy.endMethod == ADJUST_EXISTING_INPUT_WAY_END_NODES) {
                    intents.add(new ColumbinaOutputIntent.MergeExistToThisIfOK(endNode, finalNodes.get(finalNodes.size() - 1), List.of(finalWay), List.of(endWay)));
                    // UtilsUI.testMsgWindow("i == -1");
                }
                else  // 原endNodeIdx后插，插完后新点为endNodeIdx + 1
                    intents.add(new ColumbinaOutputIntent.InsertThisToExistWay(finalNodes.get(finalNodes.size() - 1), endWay, (endNodeIdx + 1) % endNodeNum));
            } else {
                intents.add(new ColumbinaOutputIntent.AddThisNodeIfOK(finalNodes.get(i)));
            }
        }
        intents.add(new ColumbinaOutputIntent.AddThisWayIfOK(finalWay));
        
        ColumbinaSingleOutput result = new ColumbinaSingleOutput(intents, List.of(finalWay), new ArrayList<>(), new HashMap<>());
        result.extraData.put("startMethod", tanNodeStrategy.startMethod);
        result.extraData.put("endMethod", tanNodeStrategy.endMethod);
        
        return result;
    }
    
    /**
     * 判断切距是否足够以确定是否需要裁切或沿伸
     * <ul><li>
     *     如果是前-切-起/终-切-后（切点在之间）：<ul>
     *     <li>如果用户选择的起点是起点线的最后一个点，或终点是终点线第一个点（端头）且用户允许沿伸或裁切后连接，就直接裁切；</li>
     *     <li>如果不是端头或不允许，则直接加点；</li></ul>
     * </li>
     * <li>
     *     如果是前-起-切/切-终-后（切点在延长线）：<ul>
     *     <li>如果用户选择的是端头，且用户允许沿伸或裁切后连接，就直接沿伸；</li>
     *     <li>如果不是端头或不允许，则失败；</li></ul>
     * </li>
     * <li>
     *     如果是切-前-起/终-后-切（切距太大救不了）：失败
     * </li></ul>
     * <p>后续两端分别判断，分别操作
     *
     * @param start 起点
     * @param startTan 起点侧切点
     * @param beforeStart 起点前一个点
     * @param isStartLastNode 起点是否是最后一个点（端头）
     * @param end 终点
     * @param endTan 终点侧切点
     * @param afterEnd 终点后一个点
     * @param isEndFirstNode 终点是否是第一个点（端头）
     * @param ableToAdjustInputNode 是否允许调整端头
     * @return 策略
     */
    private static TanNodeStrategyResult getTanNodeStrategyResult(
            boolean ableToAdjustInputNode, boolean startEndNotSame,
            ColumbinaEN beforeStart, ColumbinaEN startTan, ColumbinaEN start, boolean isStartLastNode,
            ColumbinaEN end, ColumbinaEN endTan, ColumbinaEN afterEnd, boolean isEndFirstNode
    ) {
        //          ↑            |          ↑
        //          o AS         |       AS o
        //     AE   |            |    AE    | ↙IS
        //    ←o----o IS (S/E)   |   ←o-----+-----o E
        //         / \           |          o S    \
        //        /   \          |         /        o BE
        //    BS o     o BE      |        o BS      |
        // 默认用BE→B作为起始方向、E→AE作为终点方向OK，但实测下来还需要补充的情况（不过这个确实是规则外，只是下面这样判断会更方便用户些，不急着改）：
        //  在判断完目前的情况后：
        //  如果交点不是起点路径最后一个点，那么还可能需要尝试在交点与起点路径下一个点之间开始曲线，
        //  如果交点不是终点路径第一个点，那么还可能需要尝试在终点路径上一个与交点之间结束曲线，
        //  上俩图情况就该是需要在S到AS之间开始、BE到E之间结束，和S到AS开始、E到AE结束
        // TODO：当只选了一个点，交点就是选定点时，形成T形（T下半部分为起点路径，向北），选择向左，起点端不会插入节点而是单纯addComment，需要检查原因。
        final int startMethod;
        final int endMethod;
        // 起点端
        if (ColumbinaEN.isBOnAC(beforeStart, startTan, start)) {
            if (isStartLastNode && ableToAdjustInputNode && startEndNotSame) {/*裁切逻辑*/ startMethod = ADJUST_EXISTING_INPUT_WAY_END_NODES;}
            else /*直接加节点逻辑*/ startMethod = ADD_NODES;
        } else if (ColumbinaEN.isBOnAC(beforeStart, start, startTan)) {
            if (isStartLastNode && ableToAdjustInputNode && startEndNotSame) {/*沿伸逻辑*/ startMethod = ADJUST_EXISTING_INPUT_WAY_END_NODES;}
            else startMethod = FAILED;
        } else startMethod = FAILED;
        // 终点端
        // UtilsUI.testMsgWindow("end端\n"
        //         + "end" + end + " tan" + endTan + " after" + afterEnd + "\n"
        //         + "end-tan" + new ColumbinaEN(end, endTan).bearingRad() + "\n"
        //         + "tan-after" + new ColumbinaEN(endTan, afterEnd).bearingRad() + "\n"
        //         + "end-after" + new ColumbinaEN(end, afterEnd).bearingRad() + "\n"
        // );
        if (ColumbinaEN.isBOnAC(end, endTan, afterEnd)) {
            if (isEndFirstNode && ableToAdjustInputNode) {/*裁切逻辑*/ endMethod = ADJUST_EXISTING_INPUT_WAY_END_NODES;}
            else /*直接加节点逻辑*/ endMethod = ADD_NODES;
        } else if (ColumbinaEN.isBOnAC(endTan, end, afterEnd)) {
            if (isEndFirstNode && ableToAdjustInputNode) {/*沿伸逻辑*/ endMethod = ADJUST_EXISTING_INPUT_WAY_END_NODES;}
            else endMethod = FAILED;
        } else endMethod = FAILED;
        
        return new TanNodeStrategyResult(startMethod, endMethod);
    }
    
    /**
     * 计算交点和是否平行
     * <p>如果平行，则直接以start和end中点作为交点
     * @param endDirVec 终点路径方向向量
     * @param startDirVec 终点路径方向向量
     * @param end 终点
     * @param start 起点
     * @return 交点和是否平行
     */
    private static IntersectResult getIntersectResult(ColumbinaEN start, ColumbinaEN startDirVec, ColumbinaEN end, ColumbinaEN endDirVec) {
        boolean parallel; ColumbinaEN intersect;
        
        double denominator = endDirVec.east() * startDirVec.north() - startDirVec.east() * endDirVec.north();
        
        if (Math.abs(denominator) < UtilsMath.EPSILON_EASING) {  // 分母=0则是平行
            parallel = true;
            intersect = start.centerBetween(end);  // 如果平行，则直接以start和end中点作为交点
        } else {
            double t = ((end.north() - start.north()) * endDirVec.east() - (end.east() - start.east()) * endDirVec.north())
                    / denominator;
            parallel = false;
            intersect = start.walk(startDirVec.bearingRad(), t);  // 如果不平行，直接可以计算交点
        }
        
        return new IntersectResult(parallel, intersect);
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
     * 打包切点策略
     */
    private static class TanNodeStrategyResult {
        public final int startMethod;
        public final int endMethod;
        
        public TanNodeStrategyResult(int startMethod, int endMethod) {
            this.startMethod = startMethod;
            this.endMethod = endMethod;
        }
    }
    
    /**
     * 打包交点和是否平行
     */
    private static class IntersectResult {
        public final boolean parallel;
        public final ColumbinaEN intersect;
        
        public IntersectResult(boolean parallel, ColumbinaEN intersect) {
            this.parallel = parallel;
            this.intersect = intersect;
        }
    }
}


