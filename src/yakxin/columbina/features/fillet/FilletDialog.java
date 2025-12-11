package yakxin.columbina.features.fillet;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.*;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.utils.UtilsMath;
import yakxin.columbina.utils.UtilsUI;

/// 倒圆角对话框
public final class FilletDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    // 窗体组件
    protected final JPanel panel = new JPanel(new GridBagLayout());
    private final JFormattedTextField filletR;
    private final JFormattedTextField filletChainageLength;
    private final JFormattedTextField filletMaxPointNum;
    private final JFormattedTextField minAngleDeg;
    private final JFormattedTextField maxAngleDeg;

    private final JCheckBox deleteOldWays;
    private final JCheckBox selectNewWays;
    private final JCheckBox copyTag;

    // 构建窗口
    protected FilletDialog(ColumbinaInput input) {
        // 标题、按钮
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);

        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);  // ESC取消

        // 根据输入要素计算最大半径并显示（暂定无视最小最大角限制）
        double maxRecommendedR = calculateMaximumRadius(input, 0, Math.PI);

        // 窗体
        UtilsUI.addHeader(panel, I18n.tr("Round Corners"), "RoundCorners");

        UtilsUI.addSection(panel, I18n.tr("Curve Information"));
        filletR = UtilsUI.addInput(panel, I18n.tr("Fillet (round corner) radius (m): "), String.valueOf(FilletPreference.getFilletRadius()));
        UtilsUI.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ Currently recommended maximum radius: {0}m.", maxRecommendedR)
                        + "</div></html>",
                15
        );
        filletChainageLength = UtilsUI.addInput(panel, I18n.tr("Chainage length (node spacing, m): "), String.valueOf(FilletPreference.getFilletChainageLength()));
        filletMaxPointNum = UtilsUI.addInput(panel, I18n.tr("Maximum nodes per curve segment (excluding start and end): "), String.valueOf(FilletPreference.getFilletMaxPointPerArc()));
        UtilsUI.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ The chainage length specifies the number of nodes along the curve. A shorter chainage length results in more nodes. ")
                        + I18n.tr("Note that this length is not the final distance between nodes. Instead, the required number of nodes is calculated based on the curve length and the chainage length, after which the nodes are evenly distributed according to this node count. ")
                        + I18n.tr("Furthermore, if the calculated number of nodes exceeds the maximum node limit, the node spacing will be recalculated based on the maximum node count.")
                        + "</div></html>",
                15
        );
        UtilsUI.addSpace(panel,4);
        minAngleDeg = UtilsUI.addInput(panel, I18n.tr("Minimum angle allowed for drawing curves (degrees°): "), String.valueOf(FilletPreference.getFilletMinAngleDeg()));
        maxAngleDeg = UtilsUI.addInput(panel, I18n.tr("Maximum angle allowed for drawing curves (degrees°): "), String.valueOf(FilletPreference.getFilletMaxAngleDeg()));
        UtilsUI.addLabel(
                panel,
                "<html><div style=\"width:275\">"
                        + I18n.tr("※ When the angle approaches 0°, it forms a hairpin turn; when it approaches 180°, it indicates the lines near the corner are already relatively smooth. ")
                        + I18n.tr("In both cases, rounding is usually unnecessary.")
                        + "</div></html>",
                15
        );

        UtilsUI.addSection(panel, I18n.tr("Other Operations"));
        copyTag = UtilsUI.addCheckbox(panel, I18n.tr("Copy original ways'' tags"), FilletPreference.isFilletCopyTag());
        deleteOldWays = UtilsUI.addCheckbox(panel, I18n.tr("Remove original ways after drawing"), FilletPreference.isFilletDeleteOldWays());
        selectNewWays = UtilsUI.addCheckbox(panel, I18n.tr("Select new ways after drawing"), FilletPreference.isFilletSelectNewWays());

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
            List<double[]> nodeXY = new ArrayList<>();
            List<Double> secLength = new ArrayList<>();
            boolean isClosed = way.isClosed();  // 路径是否闭合
            int numNode = isClosed ? nodes.size() - 1 : nodes.size();  // 实际节点数（去除闭合点）
            int numSec = isClosed ? numNode : numNode - 1;
            if (numNode < 3) continue;  // 至少需要3个节点才能形成拐角

            // 储存所有的节点
            for (int i = 0; i < numNode; i ++) {
                EastNorth nodeEN = nodes.get(i).getEastNorth();
                nodeXY.add(new double[] {nodeEN.getX(), nodeEN.getY()});
            }
            // 储存每段长度
            for (int i = 0; i < numSec; i ++) {
                double[] section = UtilsMath.sub(nodeXY.get((i + 1) % numNode), nodeXY.get(i));
                secLength.add(UtilsMath.norm(section));
            }

            // 对于每段折线Li，找最小半径Ri = Li / (cot(αi/2)+cot(βi/2))，αi是Li第一端拐角大小，βi是Li第二端拐角大小
            for (int i = 0; i < numSec; i ++) {
                double ang1 = UtilsMath.getAngleRadBetweenVec(
                        UtilsMath.sub(nodeXY.get((i + numNode + 1) % numNode), nodeXY.get(i)),
                        UtilsMath.sub(nodeXY.get((i + numNode - 1) % numNode), nodeXY.get(i))  // +numNode防超下界
                );
                double cotAng1 = (!isClosed && i == 0 || ang1 < Math.max(minAngleRad, 10e-6) || ang1 > Math.min(maxAngleRad, Math.PI - 10e-6)) ?
                        0 : 1 / Math.tan(ang1 / 2);  // 非闭合曲线端点记为0、发卡弯或直线记为0
                double ang2 = UtilsMath.getAngleRadBetweenVec(
                        UtilsMath.sub(nodeXY.get((i + 2) % numNode), nodeXY.get((i + 1) % numNode)),
                        UtilsMath.sub(nodeXY.get((i + 0) % numNode), nodeXY.get((i + 1) % numNode))
                );
                double cotAng2 = (!isClosed && i == numSec - 1  || ang2 < Math.max(minAngleRad, 10e-6) || ang2 > Math.min(maxAngleRad, Math.PI - 10e-6)) ?
                        0 : 1 / Math.tan(ang2 / 2);  // 非闭合曲线端点记为0、发卡弯或直线记为0
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

    public void setMinAngleDeg(double angleDeg) {minAngleDeg.setText(String.valueOf(angleDeg));}
    public void setMaxAngleDeg(double angleDeg) {maxAngleDeg.setText(String.valueOf(angleDeg));}
    public void setFilletChainageLength(double chainageLength) {
        filletChainageLength.setText(String.valueOf(chainageLength));}
}


