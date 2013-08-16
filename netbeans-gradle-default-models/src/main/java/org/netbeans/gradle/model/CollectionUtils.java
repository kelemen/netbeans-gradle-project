package org.netbeans.gradle.model;

import java.util.Collection;

public final class CollectionUtils {
    public static void checkNoNullElements(Collection<?> collection, String name) {
        if (collection == null) throw new NullPointerException(name);

        int index = 0;
        for (Object element: collection) {
            if (element == null) throw new NullPointerException(name + "[" + index + "]");
            index++;
        }
    }
}
