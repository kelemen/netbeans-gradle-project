package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollectionUtils {
    public static <E> List<E> copyNullSafeList(List<? extends E> list) {
        if (list == null) throw new NullPointerException("list");

        List<E> result = Collections.unmodifiableList(new ArrayList<E>(list));
        for (E element: list) {
            if (element == null) throw new NullPointerException("element");
        }
        return result;
    }

    private CollectionUtils() {
        throw new AssertionError();
    }
}
