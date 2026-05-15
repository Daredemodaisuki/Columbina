package yakxin.columbina.features.fillet.advanced;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import yakxin.columbina.data.ColumbinaCorner;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.features.fillet.FilletParams;
import yakxin.columbina.utils.UtilsMath;
import yakxin.columbina.utils.UtilsUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class AdvFilletDialog extends ExtendedDialog {
    private static final String[] BUTTON_TEXTS = new String[] {I18n.tr("Confirm"), I18n.tr("Cancel")};
    private static final String[] BUTTON_ICONS = new String[] {"ok", "cancel"};

    private static final double STRAIGHT_THRESHOLD_RAD = Math.toRadians(170);

    private JTable table;

    public AdvFilletDialog(FilletParams savedParams, ColumbinaInput input) {
        super(MainApplication.getMainFrame(),
                I18n.tr("Columbina"),
                BUTTON_TEXTS,
                true);
        setButtonIcons(BUTTON_ICONS);
        setDefaultButton(1);

        List<CornerInfo> corners = computeCorners(input, savedParams.surfaceRadius);

        JScrollPane tableScrollPane = makeAndGetTable(corners);

        JPanel panel = new JPanel(new GridBagLayout());
        UtilsUI.addHeader(panel,
                UtilsUI.ADVANCED_ITALIC + "\n\n"
                        + I18n.tr("Round Corners"),
                "RoundCorners"
        );

        UtilsUI.addSection(panel, I18n.tr("Corner Information"));
        panel.add(tableScrollPane, GBC.eol().fill(GridBagConstraints.HORIZONTAL));

        if (corners.isEmpty()) {
            UtilsUI.addSpace(panel, 5);
            JLabel hint = new JLabel(I18n.tr("No corners found in the selected ways."));
            hint.setForeground(Color.GRAY);
            panel.add(hint, GBC.eol());
        }

        contentInsets = new Insets(5, 15, 5, 15);
        setContent(panel);

        setupDialog();
        showDialog();
    }

    private JScrollPane makeAndGetTable(List<CornerInfo> corners) {
        String[] columnNames = {
                I18n.tr("No."), I18n.tr("Node ID"),
                I18n.tr("Angle (°)"), I18n.tr("Preferred Max Radius (m)"),
                I18n.tr("Radius (m)")
        };
        Object[][] data = buildTableData(corners);
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? String.class : Double.class;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(22);
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(520, 250));
        return tableScrollPane;
    }

    private static List<CornerInfo> computeCorners(ColumbinaInput input, double defaultRadius) {
        List<CornerInfo> corners = new ArrayList<>();
        if (input == null) return corners;

        for (Way way : input.getWays()) {
            corners.addAll(computeCornersForWay(way, defaultRadius));
        }
        return corners;
    }

    private static List<CornerInfo> computeCornersForWay(Way way, double defaultRadius) {
        List<CornerInfo> corners = new ArrayList<>();
        if (way == null) return corners;

        int numNode = way.isClosed() ? way.getNodesCount() - 1 : way.getNodesCount();
        if (numNode < 3) return corners;

        int startIdx = way.isClosed() ? 0 : 1;
        int endIdx = way.isClosed() ? numNode : numNode - 2;

        for (int i = startIdx; i <= endIdx; i++) {
            try {
                ColumbinaCorner corner = ColumbinaCorner.create(way, (i - 1 + numNode) % numNode);
                if (corner.angleRad > STRAIGHT_THRESHOLD_RAD) continue;

                double surfaceLenBA = UtilsMath.eastNorthDistanceToSurface(corner.lenBA, corner.latB);
                double surfaceLenBC = UtilsMath.eastNorthDistanceToSurface(corner.lenBC, corner.latB);
                double maxRadius = UtilsMath.getMaxRadiusForCorner(surfaceLenBA, surfaceLenBC, corner.angleRad);
                // double preferredRadius = Math.min(maxRadius, defaultRadius);
                // double preferredRadius = Math.min(maxRadius, defaultRadius);

                Node cornerNode = way.getNode(i % numNode);
                corners.add(new CornerInfo(
                        cornerNode.getUniqueId(),
                        Math.toDegrees(corner.angleRad),
                        maxRadius
                ));
            } catch (Exception ignored) {
            }
        }
        return corners;
    }

    private static Object[][] buildTableData(List<CornerInfo> corners) {
        Object[][] data = new Object[corners.size()][5];
        for (int i = 0; i < corners.size(); i++) {
            CornerInfo c = corners.get(i);
            data[i][0] = i + 1;
            data[i][1] = String.valueOf(c.nodeId);
            data[i][2] = Math.round(c.angleDeg * 100.0) / 100.0;
            data[i][3] = Math.round(c.preferredMaxRadius * 100.0) / 100.0;
            data[i][4] = Math.round(c.preferredMaxRadius * 100.0) / 100.0;
        }
        return data;
    }

    private static class CornerInfo {
        final long nodeId;
        final double angleDeg;
        final double preferredMaxRadius;

        CornerInfo(long nodeId, double angleDeg, double preferredMaxRadius) {
            this.nodeId = nodeId;
            this.angleDeg = angleDeg;
            this.preferredMaxRadius = preferredMaxRadius;
        }
    }
}
