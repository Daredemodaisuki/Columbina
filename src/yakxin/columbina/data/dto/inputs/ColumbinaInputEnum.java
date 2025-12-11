package yakxin.columbina.data.dto.inputs;

import org.openstreetmap.josm.tools.I18n;

public enum ColumbinaInputEnum {
    NODE(I18n.tr("node")),
    WAY(I18n.tr("way")),
    NODE_WAY(I18n.tr("node and way")),
    EMPTY(I18n.tr("empty input"));

    private final String descriptionI18n;

    ColumbinaInputEnum(String descriptionI18n) {
        this.descriptionI18n = descriptionI18n;
    }
    public String getDescriptionI18n() {
        return this.descriptionI18n;
    }
}
