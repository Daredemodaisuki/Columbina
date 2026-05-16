package yakxin.columbina.features.fillet;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.*;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.ColumbinaEN;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.features.fillet.advanced.AdvFilletDialog;
import yakxin.columbina.features.fillet.advanced.AdvFilletParams;
import yakxin.columbina.utils.UtilsMath;
import yakxin.columbina.utils.UtilsUI;

/// 倒圆角对话框
public final class FilletDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};
    
    // 窗体组件
    private final JFormattedTextField filletR;
    private final JFormattedTextField filletChainageLength;
    private final JFormattedTextField filletMaxPointNum;
    private final JFormattedTextField minAngleDeg;
    private final JFormattedTextField maxAngleDeg;
    private AdvFilletParams advFilletParams = null;
    
    private final JCheckBox deleteOldWays;
    private final JCheckBox selectNewWays;
    private final JCheckBox copyTag;

    FilletDialog(ColumbinaInput input, FilletParams savedParams) {
        // 标题、按钮
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);
        
        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消
        
        // 根据输入要素计算最大半径并显示
        double minAngleRad = Math.toRadians(savedParams.minAngleDeg);
        double maxAngleRad = Math.toRadians(savedParams.maxAngleDeg);
        double maxRecommendedR = calculateMaximumRadius(input, minAngleRad, maxAngleRad);
        
        // 窗体
        JPanel panel = new JPanel(new GridBagLayout());
        UtilsUI.addHeader(panel, I18n.tr("Round Corners"), "RoundCorners");
        
        UtilsUI.addSection(panel, I18n.tr("Curve Information"));
        
        filletR = UtilsUI.addInput(panel, I18n.tr("Fillet (round corner) radius (m): "), savedParams.surfaceRadius);
        UtilsUI.addLabel(panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ Currently recommended maximum radius: {0}m.", maxRecommendedR)
                        + "</div></html>",
                15
        );
        
        filletChainageLength = UtilsUI.addInput(panel, I18n.tr("Chainage length (node spacing, m): "), savedParams.surfaceChainageLength);
        filletMaxPointNum = UtilsUI.addInput(panel, I18n.tr("Maximum nodes per curve segment (excluding start and end): "), savedParams.maxPointNum);
        UtilsUI.addLabel(panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ The chainage length specifies the number of nodes along the curve. A shorter chainage length results in more nodes. ")
                        + I18n.tr("Note that this length is not the final distance between nodes. Instead, the required number of nodes is calculated based on the curve length and the chainage length, after which the nodes are evenly distributed according to this node count. ")
                        + I18n.tr("Furthermore, if the calculated number of nodes exceeds the maximum node limit, the node spacing will be recalculated based on the maximum node count.")
                        + "</div></html>",
                15
        );
        
        UtilsUI.addSpace(panel,4);
        minAngleDeg = UtilsUI.addInput(panel, I18n.tr("Minimum angle allowed for drawing curves (degrees°): "), savedParams.minAngleDeg);
        maxAngleDeg = UtilsUI.addInput(panel, I18n.tr("Maximum angle allowed for drawing curves (degrees°): "), savedParams.maxAngleDeg);
        UtilsUI.addLabel(panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ When the angle approaches 0°, it forms a hairpin turn; when it approaches 180°, it indicates the lines near the corner are already relatively smooth. ")
                        + I18n.tr("In both cases, rounding is usually unnecessary.")
                        + "</div></html>",
                15
        );

        UtilsUI.addSpace(panel, 5);
        UtilsUI.addButton(
                panel,
                I18n.tr("Advanced"),
                (ActionEvent e) -> {
                    AdvFilletDialog advFilletDialog = new AdvFilletDialog(getParams(), input);  // 传入当前输入框的参数
                    // 如果高级窗口点击确定，则记录高级参数
                    if (advFilletDialog.getValue() == 1) advFilletParams = advFilletDialog.getAdvParams();
                },
                GBC.eol().insets(0, 5, 0, 0).anchor(13)
        );

        UtilsUI.addSection(panel, I18n.tr("Other Operations"));
        copyTag = UtilsUI.addCheckbox(panel, I18n.tr("Copy original ways'' tags"), savedParams.copyTag);
        deleteOldWays = UtilsUI.addCheckbox(panel, I18n.tr("Remove original ways after drawing"), savedParams.deleteOld);
        selectNewWays = UtilsUI.addCheckbox(panel, I18n.tr("Select new ways after drawing"), savedParams.selectNew);
        
        contentInsets = new Insets(5, 15, 5, 15);  // 内容边距
        setContent(panel);
        
        // 显示
        setupDialog();
        showDialog();
    }
    
    /**
     * 计算所有选中路径中所有拐角的最大允许半径中的最小值（用于参数窗口显示）
     * @param input 输入要素
     * @param minAngleRad 最小张角
     * @param maxAngleRad 最大张角
     * @return 最大允许半径（如果无法计算或没有限制，返回Double.POSITIVE_INFINITY）
     */
    private double calculateMaximumRadius(ColumbinaInput input, double minAngleRad, double maxAngleRad) {
        double minMaxR = Double.POSITIVE_INFINITY;
        
        for (Way way : input.getWays()) {
            List<Node> nodes = way.getNodes();
            List<ColumbinaEN> nodeENs = new ArrayList<>();
            List<Double> secLength = new ArrayList<>();
            boolean isClosed = way.isClosed();  // 路径是否闭合
            int numNode = isClosed ? nodes.size() - 1 : nodes.size();  // 实际节点数（去除闭合点）
            int numSec = isClosed ? numNode : numNode - 1;
            if (numNode < 3) continue;  // 至少需要3个节点才能形成拐角
            
            // 储存所有的节点坐标
            for (int i = 0; i < numNode; i++) nodeENs.add(new ColumbinaEN(nodes.get(i).getEastNorth()));
            // 储存每段长度
            for (int i = 0; i < numSec; i++) {
                ColumbinaEN section = new ColumbinaEN(nodeENs.get(i), nodeENs.get((i + 1) % numNode));
                secLength.add(section.length());
            }
            
            // 对于每段折线Li，找最小半径Ri = Li / (cot(αi/2)+cot(βi/2))
            // αi是Li第一端拐角大小，βi是Li第二端拐角大小
            for (int i = 0; i < numSec; i++) {
                double cotAng1 = 0, cotAng2 = 0;
                if (isClosed || i > 0)  // 非闭合曲线起点第一端拐角记保持0
                    try {  // 第一端拐角：节点i-1, i, i+1
                        double ang1 = ColumbinaCorner.create(way, i + numNode - 1).angleRad;  // +numNode防止读取-1
                        // 检查角度是否在有效范围内
                        if (ang1 >= Math.max(minAngleRad, UtilsMath.EPSILON_EASING) && ang1 <= Math.min(maxAngleRad, Math.PI - UtilsMath.EPSILON_EASING)) {
                            cotAng1 = 1 / Math.tan(ang1 / 2);
                        } else cotAng1 = 0;  // 计算第一端拐角（节点i处的拐角）
                    } catch (ColumbinaException ignored) {}  // 如果无法创建拐角，拐角记为0
                if (isClosed || i < numSec - 1)  // 非闭合曲线终点第二端拐角记保持0
                    try {  // 第二端拐角：节点i, i+1, i+2
                        double ang2 = ColumbinaCorner.create(way, i).angleRad;
                        // 检查角度是否在有效范围内
                        if (ang2 >= Math.max(minAngleRad, UtilsMath.EPSILON_EASING) && ang2 <= Math.min(maxAngleRad, Math.PI - UtilsMath.EPSILON_EASING)) {
                            cotAng2 = 1 / Math.tan(ang2 / 2);
                        } else cotAng2 = 0;  // 计算第二端拐角（节点i+1处的拐角）
                    } catch (ColumbinaException ignored) {}  // 如果无法创建拐角，拐角记为0
                
                // 计算最大允许半径
                double maxRForCorner;
                if (cotAng1 + cotAng2 == 0) maxRForCorner = Double.POSITIVE_INFINITY;
                else maxRForCorner = UtilsMath.eastNorthDistanceToSurface(
                        secLength.get(i) / (cotAng1 + cotAng2),
                        nodes.get(i).lat()
                );
                if (maxRForCorner < minMaxR) minMaxR = maxRForCorner;  // 更新最小最大值
            }
        }
        return minMaxR;
    }
    
    // 获取数据
    public FilletParams getParams() {
        FilletParams filletParams = new FilletParams(
                getFilletRadius(), getFilletChainageLength(),
                getFilletMaxPointNum(), getMinAngleDeg(), getMaxAngleDeg(),
                getIfDeleteOld(), getIfSelectNew(), getIfCopyTag()
        );
        if (this.advFilletParams != null) filletParams.advFilletParams = this.advFilletParams;
        return filletParams;
    }
    
    public double getFilletRadius() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(filletR.getText()).doubleValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_RADIUS;
            // 能返回数值就返回，有异常的话返回默认值（但这里不做数值校验，在Action类中检查是否合法）
        }
    }
    public double getFilletChainageLength() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(filletChainageLength.getText()).doubleValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_CHAINAGE_LENGTH;
        }
    }
    public int getFilletMaxPointNum() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(filletMaxPointNum.getText()).intValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_MAX_POINT_PER_ARC;
        }
    }
    public double getMinAngleDeg() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(minAngleDeg.getText()).doubleValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_MIN_ANGLE_DEG;
        }
    }
    public double getMaxAngleDeg() {
        try {
            return NumberFormat.getInstance(Locale.US).parse(maxAngleDeg.getText()).doubleValue();
        } catch (ParseException e) {
            return FilletPreference.DEFAULT_FILLET_MAX_ANGLE_DEG;
        }
    }
    public boolean getIfCopyTag() {return copyTag.isSelected();}
    public boolean getIfDeleteOld() {return deleteOldWays.isSelected();}
    public boolean getIfSelectNew() {return selectNewWays.isSelected();}
}


