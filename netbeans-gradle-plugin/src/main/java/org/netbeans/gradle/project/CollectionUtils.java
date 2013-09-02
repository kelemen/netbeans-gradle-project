package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CollectionUtils {
    public static <K, V> Map<K, V> copyNullSafeHashMap(Map<? extends K, ? extends V> map) {
        return org.netbeans.gradle.model.util.CollectionUtils.copyNullSafeHashMap(map);
    }

    public static <E> List<E> copyNullSafeList(Collection<? extends E> list) {
        if (list == null) throw new NullPointerException("list");

        List<E> result = Collections.unmodifiableList(new ArrayList<E>(list));
        for (E element: result) {
            if (element == null) throw new NullPointerException("element");
        }
        return result;
    }

    public static <E> ArrayList<E> copyNullSafeMutableList(Collection<? extends E> list) {
        if (list == null) throw new NullPointerException("list");

        ArrayList<E> result = new ArrayList<E>(list);
        for (E element: result) {
            if (element == null) throw new NullPointerException("element");
        }
        return result;
    }

    private CollectionUtils() {
        throw new AssertionError();
    }
}
