package yakxin.columbina.data;

import org.openstreetmap.josm.spi.preferences.Config;

public class ColumbinaPrefItem<T> {
    final String key;
    final Class<T> type;
    final T defaultValue;
    T value;
    
    public ColumbinaPrefItem(String featureName, String keyName, Class<T> type, T defaultValue) {
        this.key = "columbina." + featureName + "." + keyName;
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }
    
    public void readFromConfig() {
        var pref = Config.getPref();
        if (type == Double.class)
            value = type.cast(pref.getDouble(key, (Double) defaultValue));
        else if (type == Integer.class)
            value = type.cast(pref.getInt(key, (Integer) defaultValue));
        else if (type == Boolean.class)
            value = type.cast(pref.getBoolean(key, (Boolean) defaultValue));
    }
    
    public void saveToConfig() {
        var pref = Config.getPref();
        if (type == Double.class)
            pref.putDouble(key, (Double) value);
        else if (type == Integer.class)
            pref.putInt(key, (Integer) value);
        else if (type == Boolean.class)
            pref.putBoolean(key, (Boolean) value);
    }
    
    
    public void setValue(T value) {
        this.value = value;
    }
    
    public T getValue() {
        return this.value;
    }
    
    public Class<T> getType() {
        return this.type;
    }
}


