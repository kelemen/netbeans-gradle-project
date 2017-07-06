package org.netbeans.gradle.project.api.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.jtrim2.collections.ArraysEx;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines a path to a subtree in a configuration tree. For most practical
 * purposes, you can consider keys in this path as nested {@literal XML} tags.
 * The keys in the path are case-sensitive.
 * <P>
 * Instances of this class are immutable and as such can be shared without any
 * further synchronization.
 * <P>
 * Instances of this class can be created through one of its factory methods.
 *
 * @see ConfigTree
 */
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

    /**
     * Returns a {@code ConfigPath} with the given keys in the path.
     *
     * @param keys the keys pointing to a subtree in a configuration tree.
     *   This argument cannot be {@code null} and the keys cannot be {@code null}.
     * @return a {@code ConfigPath} with the given keys in the path. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public static ConfigPath fromKeys(@Nonnull String... keys) {
        return keys.length > 0
                ? new ConfigPath(keys.clone())
                : ROOT;
    }

    /**
     * Returns a {@code ConfigPath} with the given keys in the path.
     *
     * @param keys the keys pointing to a subtree in a configuration tree.
     *   This argument cannot be {@code null} and the keys cannot be {@code null}.
     * @return a {@code ConfigPath} with the given keys in the path. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public static ConfigPath fromKeys(@Nonnull List<String> keys) {
        return keys.isEmpty()
                ? ROOT
                : new ConfigPath(keys.toArray(NO_KEYS));
    }

    /**
     * Returns a {@code ConfigPath} which has the starts with the given keys
     * and ends with the keys of this {@code ConfigPath}.
     *
     * @param parentKeys the keys to be prepended to the keys of this
     *   {@code ConfigPath}. This argument cannot be {@code null} and no keys
     *   can be {@code null}.
     * @return a {@code ConfigPath} which has the starts with the given keys
     *   and ends with the keys of this {@code ConfigPath}. This method never
     *   returns {@code null}.
     */
    @Nonnull
    public ConfigPath withParentPath(@Nonnull String... parentKeys) {
        List<String> newKeys = new ArrayList<>(keys.length + parentKeys.length);
        newKeys.addAll(Arrays.asList(parentKeys));
        newKeys.addAll(keysAsList);
        return ConfigPath.fromKeys(newKeys);
    }

    /**
     * Returns a {@code ConfigPath} which has the starts with the keys of this
     * {@code ConfigPath} and ends with the given keys.
     *
     * @param childKeys the keys to be appended to the keys of this
     *   {@code ConfigPath}. This argument cannot be {@code null} and no keys
     *   can be {@code null}.
     * @return a {@code ConfigPath} which has the starts with the keys of this
     *   {@code ConfigPath} and ends with the given keys. This method never
     *   returns {@code null}.
     */
    @Nonnull
    public ConfigPath withChildPath(@Nonnull String... childKeys) {
        List<String> newKeys = new ArrayList<>(keys.length + childKeys.length);
        newKeys.addAll(keysAsList);
        newKeys.addAll(Arrays.asList(childKeys));
        return ConfigPath.fromKeys(newKeys);
    }

    /**
     * Returns the number of keys found in this {@code ConfigPath}. That is,
     * the same number as {@code getKeys().size()}.
     *
     * @return the number of keys found in this {@code ConfigPath}. This method
     *   always returns a value greater than or equal to zero.
     */
    public int getKeyCount() {
        return keys.length;
    }

    /**
     * Returns the key at the given index in the {@link #getKeys() keys list}
     * of this {@code ConfigPath}.
     *
     * @param index the index of the requested key. This index must be greater
     *   or equal to zero and less than {@link #getKeyCount() getKeyCount()}.
     * @return the key at the given index in the {@link #getKeys() keys list}
     *   of this {@code ConfigPath}. This method never returns {@code null}.
     */
    @Nonnull
    public String getKeyAt(int index) {
        return keys[index];
    }

    /**
     * Returns the keys of this {@code ConfigPath} defining the edges in a
     * {@link ConfigTree}.
     *
     * @return the keys of this {@code ConfigPath} defining the edges in a
     *   {@link ConfigTree}. This method never returns {@code null} and none
     *   of the keys are {@code null} as well.
     */
    @Nonnull
    public List<String> getKeys() {
        return keysAsList;
    }

    /**
     * Returns {@code true} if the given {@code ConfigPath} is the child path
     * of this {@code ConfigPath}, {@code false} otherwise. That is, if the keys
     * of the given {@code ConfigPath} starts with the keys of this
     * {@code ConfigPath}. If the given {@code ConfigPath} specifies the same
     * path as this {@code ConfigPath}, this method returns {@code true}.
     *
     * @param childCandidate the {@code ConfigPath} to be tested if it is a
     *   child path of this {@code ConfigPath}. This argument cannot be
     *   {@code null}.
     * @return {@code true} if the given {@code ConfigPath} is the child path
     *   of this {@code ConfigPath}, {@code false} otherwise
     */
    public boolean isParentOfOrEqual(@Nonnull ConfigPath childCandidate) {
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

    /**
     * Returns a hash code compatible with the {@code equals} method.
     *
     * @return a hash code compatible with the {@code equals} method
     */
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

    /**
     * Returns {@code true} if the passed object is a {@code ConfigPath} and
     * it has the same {@link #getKeys() keys} as this path, {@code false}
     * otherwise.
     *
     * @param obj the object to be tested for equality. This argument might
     *   be {@code null}, in which case the return value is {@code false}.
     * @return {@code true} if the passed object is a {@code ConfigPath} and
     *   it has the same {@link #getKeys() keys} as this path, {@code false}
     *   otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final ConfigPath other = (ConfigPath)obj;
        return Arrays.equals(this.keys, other.keys);
    }

    /**
     * Returns the string representation of this {@code ConfigPath} in no
     * particular format. The return value is intended to be used for debugging.
     *
     * @return the string representation of this {@code ConfigPath} in no
     *   particular format. This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return Arrays.toString(keys);
    }
}
