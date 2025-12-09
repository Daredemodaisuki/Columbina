package yakxin.columbina.utils;

import org.openstreetmap.josm.command.*;
import org.openstreetmap.josm.data.osm.*;

import java.util.ArrayList;
import java.util.List;

public class UtilsData {
    /// 数据相关
    public static void wayReplaceNode(Way way, int index, Node newNode) {
        way.removeNode(way.getNode(index));
        way.addNode(index, newNode);
    }

    public static List<Command> tryGetCommandsFromSeqCmd (SequenceCommand seqCmd) {
        List<Command> commands = new ArrayList<>();
        for (PseudoCommand pc : seqCmd.getChildren()) {
            if (pc instanceof Command) {
                commands.add((Command) pc);
            }
        }
        return commands;
    }

    // /**
    //  * 获取选中的图层、数据库和路径
    //  * 如果图层、数据库有问题或没有选中路径，将抛出IllegalArgumentException异常
    //  * @return 获取到的图层、数据库和输入要素（Node、Way或Relation）
    //  */
    // public static <InputFeatureType extends OsmPrimitive> LayerDatasetAndFeatureSelected<InputFeatureType>
    // getLayerDatasetAndFeaturesSelected (
    //         Class<InputFeatureType> type,
    //         int minSelection, int maxSelection
    // ) {
    //     OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();  // 当前的编辑图层
    //     if (layer == null) throw new ColumbinaException(I18n.tr("Current layer is not available."));
//
    //     DataSet dataset = MainApplication.getLayerManager().getEditDataSet();  // 当前的编辑数据库
    //     if (dataset == null) throw new ColumbinaException(I18n.tr("Current dataset is not available."));
//
    //     // List<Way> waySelection = new ArrayList<>(dataset.getSelectedWays());  // 未测试的方法
//
    //     // 筛选输入类型
    //     List<InputFeatureType> selection = new ArrayList<>();
    //     for (OsmPrimitive p : layer.data.getSelected()) {if (type.isInstance(p)) selection.add(type.cast(p));}
//
    //     // 数量检查
    //     String typeNameI18n;
    //     if (type == Node.class) typeNameI18n = I18n.tr("node");
    //     else if (type == Way.class) typeNameI18n = I18n.tr("way");
    //     else typeNameI18n = I18n.tr("relation");
    //     if (selection.isEmpty()) throw new IllegalArgumentException(I18n.tr("No {0} is selected.", typeNameI18n));
    //     // 最小最大选择数量检查（有限制时）
    //     if (minSelection != AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM && selection.size() < minSelection) throw new IllegalArgumentException(I18n.tr("Too few {0}s are selected, should be grater than {1}.", typeNameI18n, minSelection));
    //     if (maxSelection != AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM && selection.size() > maxSelection) throw new IllegalArgumentException(I18n.tr("Too many {0}s are selected, should be less than {1}.", typeNameI18n, maxSelection));
    //     if (selection.size() > 5) {
    //         int confirmTooMany = JOptionPane.showConfirmDialog(
    //                 null,
    //                 I18n.tr("Are you sure you want to process {0} {1}s at once? This may take a long time.", selection.size(), typeNameI18n),
    //                 I18n.tr("Columbina"),
    //                 JOptionPane.YES_NO_OPTION
    //         );
    //         if (confirmTooMany == JOptionPane.NO_OPTION) return null;
    //     }
//
    //     return new LayerDatasetAndFeatureSelected<InputFeatureType>(layer, dataset, selection);
    // }

}


