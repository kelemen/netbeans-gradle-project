package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jtrim.collections.ArraysEx;
import org.jtrim.utils.ExceptionHelper;

public final class ConfigPath {
    private static final String[] NO_KEYS = new String[0];
    public static final ConfigPath ROOT = new ConfigPath(NO_KEYS, Collections.<String>emptyList());

    private final String[] keys;
    private final List<String> keysAsList;

    // True correctness depends on this variable not being explicitly initialized.
    private int hashCache;

    private ConfigPath(String[] keys) {
        this(keys, ArraysEx.viewAsList(keys));

        ExceptionHelper.checkNotNullElements(this.keys, "keys");
    }

    private ConfigPath(String[] keys, List<String> keysAsList) {
        this.keys = keys;
        this.keysAsList = keysAsList;
    }

    public static ConfigPath fromKeys(String... keys) {
        return keys.length > 0
                ? new ConfigPath(keys.clone())
                : ROOT;
    }

    public static ConfigPath fromKeys(List<String> keys) {
        return keys.isEmpty()
                ? ROOT
                : new ConfigPath(keys.toArray(NO_KEYS));
    }

    public ConfigPath withParentPath(String... parentKeys) {
        List<String> newKeys = new ArrayList<>(keys.length + parentKeys.length);
        newKeys.addAll(Arrays.asList(parentKeys));
        newKeys.addAll(keysAsList);
        return ConfigPath.fromKeys(newKeys);
    }

    public ConfigPath getChildPath(String... childKeys) {
        List<String> newKeys = new ArrayList<>(keys.length + childKeys.length);
        newKeys.addAll(keysAsList);
        newKeys.addAll(Arrays.asList(childKeys));
        return ConfigPath.fromKeys(newKeys);
    }

    public int getKeyCount() {
        return keys.length;
    }

    public String getKeyAt(int index) {
        return keys[index];
    }

    public List<String> getKeys() {
        return keysAsList;
    }

    public boolean isParentOfOrEqual(ConfigPath childCandidate) {
        String[] childCandidateKeys = childCandidate.keys;
        if (childCandidateKeys.length < keys.length) {
            return false;
        }

        for (int i = 0; i < keys.length; i++) {
            if (!Objects.equals(keys[i], childCandidateKeys[i])) {
                return false;
            }
        }
        return true;
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

    @Override
    public String toString() {
        return Arrays.toString(keys);
    }
}
