package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MultiKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Object> keys;

    private MultiKey(List<?> keys) {
        this.keys = new ArrayList<Object>(keys);
    }

    public static MultiKey create(Object... keys) {
        return new MultiKey(Arrays.asList(keys));
    }

    public static MultiKey createFromList(List<?> keys) {
        return new MultiKey(keys);
    }

    public Object[] getKeys() {
        return keys.toArray();
    }

    @Override
    public int hashCode() {
        return 185 + keys.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final MultiKey other = (MultiKey)obj;
        return this.keys.equals(other.keys);
    }
}
