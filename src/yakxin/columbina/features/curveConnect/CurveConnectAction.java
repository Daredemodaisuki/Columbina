package yakxin.columbina.features.curveConnect;

import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.abstractClasses.actionMiddle.ActionWithNodeWay;
import yakxin.columbina.data.dto.inputs.ColumbinaInput;
import yakxin.columbina.data.dto.inputs.ColumbinaSingleInput;

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

    @Override
    public String getUndoRedoInfo(ColumbinaInput inputs, CurveConnectParams params) {
        return "";
    }

    @Override
    public int checkInputDetails(List<ColumbinaSingleInput> singleInputs) {
        // 判断节点是否在路径上，并判断路径是否是平行的，这里可以顺便算出交点？
        return 0;
    }
}
