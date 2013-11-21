package org.netbeans.gradle.model.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CollectionUtils {
    public static void checkNoNullElements(Collection<?> collection, String name) {
        if (collection == null) throw new NullPointerException(name);

        int index = 0;
        for (Object element: collection) {
            if (element == null) throw new NullPointerException(name + "[" + index + "]");
            index++;
        }
    }

    public static <K> Map<K, List<?>> copyNullSafeMultiHashMap(Map<? extends K, List<?>> map) {
        if (map == null) throw new NullPointerException("map");

        Map<K, List<?>> result = copyNullSafeMutableHashMap(map);
        for (Map.Entry<K, List<?>> entry: result.entrySet()) {
            List<?> value = entry.getValue();
            entry.setValue(Collections.unmodifiableList(new ArrayList<Object>(value)));
        }
        return result;
    }

    public static <K, V> Map<K, V> copyNullSafeMutableHashMap(Map<? extends K, ? extends V> map) {
        if (map == null) throw new NullPointerException("map");

        Map<K, V> result = new HashMap<K, V>(map);
        for (Map.Entry<K, V> entry: result.entrySet()) {
            if (entry.getKey() == null) throw new NullPointerException("entry.getKey()");
            if (entry.getValue()== null) throw new NullPointerException("entry.getValue()");
        }
        return result;
    }

    public static <K, V> Map<K, V> copyNullSafeHashMap(Map<? extends K, ? extends V> map) {
        return Collections.unmodifiableMap(copyNullSafeMutableHashMap(map));
    }

    public static <E> List<E> copyNullSafeList(Collection<? extends E> list) {
        return Collections.unmodifiableList(copyNullSafeMutableList(list));
    }

    public static <E> ArrayList<E> copyNullSafeMutableList(Collection<? extends E> list) {
        if (list == null) throw new NullPointerException("list");

        ArrayList<E> result = new ArrayList<E>(list);
        for (E element: result) {
            if (element == null) throw new NullPointerException("element");
        }
        return result;
    }

    public static Object getSingleElement(List<?> list) {
        if (list == null) {
            return null;
        }

        int listSize = list.size();
        if (listSize > 1) {
            throw new IllegalArgumentException("Expected only one entry in the list.");
        }

        return listSize == 1 ? list.get(0) : null;
    }

    private static int expectedSizeToCapacity(int expectedSize) {
        return 4 * expectedSize / 3 + 1;
    }

    public static <K, V> Map<K, V> newHashMap(int expectedSize) {
        return new HashMap<K, V>(expectedSizeToCapacity(expectedSize));
    }

    public static <V> Set<V> newHashSet(int expectedSize) {
        return new HashSet<V>(expectedSizeToCapacity(expectedSize));
    }

    public static <V> Set<V> newLinkedHashSet(int expectedSize) {
        return new LinkedHashSet<V>(expectedSizeToCapacity(expectedSize));
    }

    private CollectionUtils() {
        throw new AssertionError();
    }
}
