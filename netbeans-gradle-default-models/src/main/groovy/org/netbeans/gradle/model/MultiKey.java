package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public final class MultiKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Object[] keys;

    private MultiKey(Object[] keys) {
        this.keys = keys.clone();
    }

    public static MultiKey create(Object... keys) {
        return new MultiKey(keys);
    }

    public static MultiKey createFromList(List<?> keys) {
        return new MultiKey(keys.toArray());
    }

    public Object[] getKeys() {
        return keys.clone();
    }

    @Override
    public int hashCode() {
        return 185 + Arrays.hashCode(keys);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final MultiKey other = (MultiKey)obj;
        return Arrays.equals(this.keys, other.keys);
    }
}
