package yakxin.columbina.abstractClasses;

public abstract class AbstractParams {
    public boolean deleteOld;
    public boolean selectNew;
    public boolean copyTag;

    protected AbstractParams() {
        this(false, true, false);  // 如果是完全新画、不涉及删除的，默认可以用这组
    }
    protected AbstractParams(boolean selectNew) {
        this(false, selectNew, false);
    }
    protected AbstractParams(boolean deleteOld, boolean selectNew, boolean copyTag) {
        this.deleteOld = deleteOld;
        this.selectNew = selectNew;
        this.copyTag = copyTag;
    }
}
