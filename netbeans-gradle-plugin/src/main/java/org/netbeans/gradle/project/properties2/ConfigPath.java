package org.netbeans.gradle.project.properties2;

import java.util.Arrays;
import java.util.List;
import org.jtrim.collections.ArraysEx;
import org.jtrim.utils.ExceptionHelper;

public final class ConfigPath {
    public static final ConfigPath ROOT = new ConfigPath(new ConfigKey[0]);

    private final ConfigKey[] keys;
    private final List<ConfigKey> keysAsList;

    // True correctness depends on this variable not being explicitly initialized.
    private int hashCache;

    private ConfigPath(ConfigKey[] keys) {
        this.keys = keys.clone();
        this.keysAsList = ArraysEx.viewAsList(this.keys);

        ExceptionHelper.checkNotNullElements(this.keys, "keys");
    }

    public static ConfigPath fromKeys(ConfigKey... keys) {
        return keys.length > 0
                ? new ConfigPath(keys)
                : ROOT;
    }

    public List<ConfigKey> getKeys() {
        return keysAsList;
    }

    @Override
    public int hashCode() {
        int hash = hashCache;
        if (hash == 0) {
            hash = 5;
            hash = 61 * hash + Arrays.hashCode(keys);
            hashCache = hash;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final ConfigPath other = (ConfigPath)obj;
        return Arrays.equals(this.keys, other.keys);
    }
}
