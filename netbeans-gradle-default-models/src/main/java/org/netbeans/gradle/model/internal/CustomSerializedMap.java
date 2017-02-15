package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class CustomSerializedMap implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final CustomSerializedMap EMPTY
            = new CustomSerializedMap.Builder(0).create();

    public static interface Deserializer {
        public Map<Object, List<?>> deserialize(
                SerializationCache serializationCache,
                ClassLoader parent,
                IssueTransformer deserializationIssueTransformer);
    }

    public static final class Builder {
        private final Map<Object, List<Object>> map;

        public Builder(int expectedSize) {
            this.map = CollectionUtils.newHashMap(expectedSize);
        }

        public void addValues(Object key, Collection<?> values) {
            List<Object> valueContainer = map.get(key);
            if (valueContainer == null) {
                valueContainer = new ArrayList<Object>();
                map.put(key, valueContainer);
            }
            valueContainer.addAll(values);
        }

        public void addValue(Object key, Object value) {
            List<Object> valueContainer = map.get(key);
            if (valueContainer == null) {
                valueContainer = new ArrayList<Object>();
                map.put(key, valueContainer);
            }
            valueContainer.add(value);
        }

        public CustomSerializedMap create() {
            return new CustomSerializedMap(this, null);
        }

        public CustomSerializedMap create(Map<Object, Throwable> serializationProblems) {
            if (serializationProblems == null) throw new NullPointerException("serializationProblems");

            return new CustomSerializedMap(this, serializationProblems);
        }
    }

    private final Map<Object, SerializedEntries> map;
    private final Map<Object, Throwable> serializationProblems;

    private CustomSerializedMap(Builder builder, Map<Object, Throwable> issueResult) {
        Map<Object, Throwable> problems = issueResult;

        Map<Object, SerializedEntries> mutableMap = CollectionUtils.newHashMap(builder.map.size());
        for (Map.Entry<Object, List<Object>> entry: builder.map.entrySet()) {
            Object key = entry.getKey();
            List<Object> value = entry.getValue();

            SerializedEntries entries;
            try {
                entries = new SerializedEntries(value);
            } catch (Throwable ex) {
                if (problems == null) {
                    problems = new HashMap<Object, Throwable>();
                }
                problems.put(key, TransferableExceptionWrapper.wrap(ex));
                continue;
            }
            mutableMap.put(entry.getKey(), entries);
        }

        this.map = Collections.unmodifiableMap(mutableMap);
        this.serializationProblems = issueResult == null && problems != null
                ? Collections.unmodifiableMap(problems)
                : null;
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

    public Map<Object, Throwable> getSerializationProblems() {
        return serializationProblems != null
                ? serializationProblems
                : Collections.<Object, Throwable>emptyMap();
    }
}
