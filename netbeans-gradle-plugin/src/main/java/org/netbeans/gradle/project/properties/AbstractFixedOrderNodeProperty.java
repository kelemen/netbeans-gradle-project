package org.netbeans.gradle.project.properties;

import java.util.Map;
import java.util.Objects;
import org.jtrim2.collections.CollectionsEx;

abstract class AbstractFixedOrderNodeProperty extends AbstractConfigNodeProperty {
    private final Map<String, Integer> order;

    public AbstractFixedOrderNodeProperty(String... order) {
        this.order = CollectionsEx.newHashMap(order.length);
        for (int i = 0; i < order.length; i++) {
            String key = order[i];
            Objects.requireNonNull(key, "order[?]");

            this.order.put(key, i);
        }
    }

    @Override
    public final int compare(String o1, String o2) {
        Integer index1 = order.get(o1);
        Integer index2 = order.get(o2);

        if (index1 == null) {
            return index2 == null
                    ? DefaultConfigNodeProperty.INSTANCE.compare(o1, o2)
                    : 1;
        }
        if (index2 == null) {
            return -1;
        }

        return Integer.compare(index1, index2);
    }
}
