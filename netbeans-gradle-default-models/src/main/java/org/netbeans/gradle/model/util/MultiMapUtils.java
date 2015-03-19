package org.netbeans.gradle.model.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class MultiMapUtils {
    public static <Key, Value> void addToMultiMap(Key key, Value value, Map<Key, List<Value>> result) {
        addAllToMultiMap(key, Collections.singletonList(value), result);
    }

    public static <Key, Value> void addAllToMultiMap(Key key, List<? extends Value> values, Map<Key, List<Value>> result) {
        List<Value> container = result.get(key);
        if (container == null) {
            container = new LinkedList<Value>();
            result.put(key, container);
        }
        container.addAll(values);
    }

    private MultiMapUtils() {
        throw new AssertionError();
    }
}
