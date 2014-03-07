package org.netbeans.gradle.project.properties2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jtrim.collections.ArraysEx;
import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ConfigPath {
    private static final ConfigKey[] NO_KEYS = new ConfigKey[0];
    public static final ConfigPath ROOT = new ConfigPath(NO_KEYS, Collections.<ConfigKey>emptyList());

    private final ConfigKey[] keys;
    private final List<ConfigKey> keysAsList;

    // True correctness depends on this variable not being explicitly initialized.
    private int hashCache;

    private ConfigPath(ConfigKey[] keys) {
        this(keys, ArraysEx.viewAsList(keys));

        ExceptionHelper.checkNotNullElements(this.keys, "keys");
    }

    private ConfigPath(ConfigKey[] keys, List<ConfigKey> keysAsList) {
        this.keys = keys;
        this.keysAsList = keysAsList;
    }

    public static ConfigPath fromKeys(ConfigKey... keys) {
        return keys.length > 0
                ? new ConfigPath(keys.clone())
                : ROOT;
    }

    public static ConfigPath fromKeys(List<ConfigKey> keys) {
        return keys.isEmpty()
                ? ROOT
                : new ConfigPath(keys.toArray(NO_KEYS));
    }

    public int getKeyCount() {
        return keys.length;
    }

    public ConfigKey getKeyAt(int index) {
        return keys[index];
    }

    public List<ConfigKey> getKeys() {
        return keysAsList;
    }

    public boolean isParentOfOrEqual(ConfigPath childCandidate) {
        ConfigKey[] childCandidateKeys = childCandidate.keys;
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

    public Element tryGetChildElement(Element root) {
        ExceptionHelper.checkNotNullArgument(root, "root");

        Node result = root;
        for (ConfigKey key: keys) {
            result = key.tryGetChildNode(result);
            if (result == null) {
                return null;
            }
        }

        return result instanceof Element ? (Element)result : null;
    }

    public Element addToNode(Element root) {
        ExceptionHelper.checkNotNullArgument(root, "root");

        Element result = root;
        for (ConfigKey key: keys) {
            result = key.addChildIfNeeded(result);
        }
        return result;
    }

    public boolean removeFromNode(Node root) {
        ExceptionHelper.checkNotNullArgument(root, "root");

        return removePath(null, root, keys, 0);
    }

    private static boolean removePath(Node parent, Node current, ConfigKey[] pathKeys, int offset) {
        if (offset >= pathKeys.length) {
            if (parent != null) {
                parent.removeChild(current);
                return true;
            }
            return false;
        }

        ConfigKey pathKey = pathKeys[offset];

        Node childNode = pathKey.tryGetChildNode(current);
        if (childNode != null) {
            return removePath(current, childNode, pathKeys, offset + 1);
        }
        return false;
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
