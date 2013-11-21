package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class CustomSerializedMap implements Serializable {
    private static final long serialVersionUID = 1L;

    public static interface Deserializer {
        public Map<Object, List<?>> deserialize(ClassLoader parent);
    }

    public static final class Builder {
        private final Map<Object, List<Object>> map;

        public Builder(int expectedSize) {
            this.map = CollectionUtils.newHashMap(expectedSize);
        }

        public void addValues(Object key, Collection<?> values) {
            List<Object> valueContainer = map.get(key);
            if (valueContainer == null) {
                valueContainer = new LinkedList<Object>();
                map.put(key, valueContainer);
            }
            valueContainer.addAll(values);
        }

        public void addValue(Object key, Object value) {
            List<Object> valueContainer = map.get(key);
            if (valueContainer == null) {
                valueContainer = new LinkedList<Object>();
                map.put(key, valueContainer);
            }
            valueContainer.add(value);
        }

        public CustomSerializedMap create() {
            return new CustomSerializedMap(this);
        }
    }

    private final Map<Object, SerializedEntries> map;

    private CustomSerializedMap(Builder builder) {
        Map<Object, SerializedEntries> mutableMap = CollectionUtils.newHashMap(builder.map.size());
        for (Map.Entry<Object, List<Object>> entry: builder.map.entrySet()) {
            List<Object> value = entry.getValue();
            mutableMap.put(entry.getKey(), new SerializedEntries(value));
        }

        this.map = Collections.unmodifiableMap(mutableMap);
    }

    public static <V> CustomSerializedMap fromMap(Map<?, List<V>> map) {
        CustomSerializedMap.Builder result = new Builder(map.size());
        for (Map.Entry<?, List<V>> entry: map.entrySet()) {
            result.addValues(entry.getKey(), entry.getValue());
        }
        return result.create();
    }

    public int size() {
        return map.size();
    }

    public Map<Object, SerializedEntries> getMap() {
        return map;
    }
}
