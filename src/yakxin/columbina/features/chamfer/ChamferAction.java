package yakxin.columbina.features.chamfer;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;

import yakxin.columbina.abstractClasses.AbstractDrawingAction;
import yakxin.columbina.abstractClasses.actionMiddle.ActionWithBatchWays;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;

import java.awt.event.KeyEvent;

public final class ChamferAction extends
        ActionWithBatchWays<ChamferGenerator, ChamferPreference, ChamferParams>
{
    // TODO:距离模式的角度检查

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
    private ChamferAction(String name, String iconName, String description, Shortcut shortcut, ChamferGenerator generator, ChamferPreference preference) {
        super(
                name, iconName, description, shortcut,
                generator, preference,
                AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM, AbstractDrawingAction.NO_LIMITATION_ON_INPUT_NUM
        );
    }

    /**
     * 懒得在主类填参数就用静态工厂方法
     * @return 构建好的实例
     */
    public static ChamferAction create() {
        return new ChamferAction(
                I18n.tr("Chamfer Corners"), "ChamferCorners",
                I18n.tr("Chamfer corners of selected ways with specified distances or angle."),
                Shortcut.registerShortcut(
                        "tools:chamferCorners",
                        "More tools: Columbina/Chamfer corners",
                        KeyEvent.VK_X,
                        Shortcut.ALT_CTRL_SHIFT
                ),
                new ChamferGenerator(),
                new ChamferPreference()
        );
    }

    @Override
    public String getUndoRedoInfo(ColumbinaInput inputs, ChamferParams params) {
        String undoRedoInfo;
        if (params.mode == ChamferGenerator.USING_DISTANCE) {
            if (inputs.getInputNum(Way.class) == 1) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}m", inputs.getWays().getFirst().getUniqueId(), params.surfaceDistanceA, params.surfaceDistanceC);
            else if (inputs.getInputNum(Way.class) <= 5) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}m", inputs.getWays().stream().map(Way::getUniqueId).toList(), params.surfaceDistanceA, params.surfaceDistanceC);
            else undoRedoInfo = I18n.tr("Chamfer of {0} ways: {1}m, {2}m", inputs.getInputNum(Way.class), params.surfaceDistanceA, params.surfaceDistanceC);
        } else {
            if (inputs.getInputNum(Way.class) == 1) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}°", inputs.getWays().getFirst().getUniqueId(), params.surfaceDistanceA, params.angleADeg);
            else if (inputs.getInputNum(Way.class) <= 5) undoRedoInfo = I18n.tr("Chamfer of way {0}: {1}m, {2}°", inputs.getWays().stream().map(Way::getUniqueId).toList(), params.surfaceDistanceA, params.angleADeg);
            else undoRedoInfo = I18n.tr("Chamfer of {0} ways: {1}m, {2}°", inputs.getInputNum(Way.class), params.surfaceDistanceA, params.angleADeg);
        }
        return undoRedoInfo;
    }
}


