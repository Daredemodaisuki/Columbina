package yakxin.columbina.chamfer;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import yakxin.columbina.data.ColumbinaException;
import yakxin.columbina.data.preference.ChamferPreference;
import yakxin.columbina.utils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ChamferAction extends JosmAction {
    private static final Shortcut shortcutChamferCorner = Shortcut.registerShortcut(
            "tools:chamferCorners",
            "More tools: Columbina/Chamfer corners",
            KeyEvent.VK_X,
            Shortcut.ALT_CTRL_SHIFT
    );

    /**
     * 构建菜单实例（构造函数）
      */
    public ChamferAction() {
        // 调用父类构造函数设置动作属性
        super(
                I18n.tr("Chamfer Corners"),  // 菜单显示文本
                "ChamferCorners",  // 图标
                I18n.tr("Chamfer corners of selected ways with specified distances or angle."),  // 工具提示
                shortcutChamferCorner,  // 快捷键
                true,  // 启用工具栏按钮
                false
        );
    }

    /**
     * 弹出参数对话框并获取参数、完成后保存设置
     */
    private ChamferParams getParams() {
        ChamferDialog chamferDialog = new ChamferDialog();
        if (chamferDialog.getValue() != 1) return null;  // 按ESC（0）或点击取消（2），退出；点击确定继续是1

        double distanceA = chamferDialog.getChamferDistanceA();
        double distanceB = chamferDialog.getChamferDistanceB();
        double angleADeg = chamferDialog.getChamferAngleADeg();

        // 保存设置
        ChamferPreference.setPreferenceFromDialog(chamferDialog);
        ChamferPreference.savePreference();

        return new ChamferParams(
                distanceA, distanceB, angleADeg,
                chamferDialog.getIfDeleteOld(), chamferDialog.getIfSelectNew(), chamferDialog.getIfCopyTag()
        );
    }

    /**
     * 点击事件
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // 输入参数
            final ChamferParams chamferParams = getParams();
            if (chamferParams == null) return;  // 用户取消操作
            utils.testMsgWindow(String.valueOf(chamferParams.distanceA));
        } catch (ColumbinaException | IllegalArgumentException exCheck) {
            utils.errorInfo(exCheck.getMessage());
        }


    }

    private static final class ChamferParams {
        public final double distanceA;
        public final double distanceB;
        public final double angleADeg;
        public final boolean deleteOld;
        public final boolean selectNew;
        public final boolean copyTag;

        ChamferParams(
                double distanceA, double distanceB,
                double angleADeg,
                boolean deleteOld, boolean selectNew, boolean copyTag
        ) {
            this.distanceA = distanceA;
            this.distanceB = distanceB;
            this.angleADeg = angleADeg;
            this.deleteOld = deleteOld;
            this.selectNew = selectNew;
            this.copyTag = copyTag;
        }
    }
}


