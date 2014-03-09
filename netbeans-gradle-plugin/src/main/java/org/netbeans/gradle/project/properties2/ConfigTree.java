package org.netbeans.gradle.project.properties2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class ConfigTree {
    public static final ConfigTree EMPTY = singleValue(null);

    public static final class Builder {
        private String value;
        private Map<ConfigKey, ConfigTree> subTrees;
        private Map<ConfigKey, ConfigTree.Builder> subTreeBuilders;
        private ConfigTree cachedBuilt;

        public Builder(@Nonnull ConfigTree initialValue) {
            this();

            value = initialValue.value;

            if (!initialValue.subTrees.isEmpty()) {
                getSubTrees().putAll(initialValue.subTrees);
            }
        }

        public Builder() {
            this.value = null;
            this.subTreeBuilders = null;
            this.subTrees = null;
            this.cachedBuilt = null;
        }

        private Map<ConfigKey, ConfigTree.Builder> getSubTreeBuilders() {
            Map<ConfigKey, ConfigTree.Builder> result = subTreeBuilders;
            if (result == null) {
                result = new HashMap<>();
                subTreeBuilders = result;
            }
            return result;
        }

        private Map<ConfigKey, ConfigTree> getSubTrees() {
            Map<ConfigKey, ConfigTree> result = subTrees;
            if (result == null) {
                result = new HashMap<>();
                subTrees = result;
            }
            return result;
        }

        public void setValue(@Nullable String value) {
            cachedBuilt = null;
            this.value = value;
        }

        public void setChildTree(@Nonnull ConfigKey key, @Nonnull ConfigTree tree) {
            ExceptionHelper.checkNotNullArgument(key, "key");
            ExceptionHelper.checkNotNullArgument(tree, "tree");

            if (subTreeBuilders != null) {
                subTreeBuilders.remove(key);
            }

            cachedBuilt = null;
            getSubTrees().put(key, tree);
        }

        public Builder getDeepSubBuilder(@Nonnull String... keyNames) {
            Builder result = this;
            for (String keyName: keyNames) {
                result = result.getSubBuilder(keyName);
            }
            return result;
        }

        public Builder getSubBuilder(@Nonnull String keyName) {
            return getSubBuilder(new ConfigKey(keyName, null));
        }

        public Builder getDeepSubBuilder(@Nonnull ConfigPath path) {
            Builder result = this;
            for (ConfigKey key: path.getKeys()) {
                result = result.getSubBuilder(key);
            }
            return result;
        }

        public Builder getDeepSubBuilder(@Nonnull ConfigKey... keys) {
            Builder result = this;
            for (ConfigKey key: keys) {
                result = result.getSubBuilder(key);
            }
            return result;
        }

        public Builder getSubBuilder(@Nonnull ConfigKey key) {
            Map<ConfigKey, Builder> childBuilders = getSubTreeBuilders();
            Builder result = childBuilders.get(key);
            if (result == null) {
                cachedBuilt = null;

                ConfigTree currentTree = subTrees.remove(key);

                result = currentTree != null
                        ? new Builder(currentTree)
                        : new Builder();
                childBuilders.put(key, result);
            }

            return result;
        }

        public void detachSubTreeBuilders() {
            if (subTreeBuilders == null || subTreeBuilders.isEmpty()) {
                return;
            }

            Map<ConfigKey, ConfigTree> currentSubTrees = getSubTrees();
            for (Map.Entry<ConfigKey, Builder> entry: subTreeBuilders.entrySet()) {
                ConfigKey key = entry.getKey();
                ConfigTree tree = entry.getValue().create();

                currentSubTrees.put(key, tree);
            }
            subTreeBuilders = null;

            cachedBuilt = create();
        }

        public ConfigTree create() {
            ConfigTree result = cachedBuilt;
            if (result == null) {
                result = new ConfigTree(this);
            }
            return result;
        }

        private void addFromTreeBuilders(Map<ConfigKey, ConfigTree> result) {
            if (subTreeBuilders == null) {
                return;
            }

            for (Map.Entry<ConfigKey, ConfigTree.Builder> entry: subTreeBuilders.entrySet()) {
                ConfigTree subTree = entry.getValue().create();
                if (subTree.hasValues()) {
                    result.put(entry.getKey(), subTree);
                }
            }
        }

        private Map<ConfigKey, ConfigTree> getChildTreesSnapshot() {
            int subTreeBuildersCount = subTreeBuilders != null ? subTreeBuilders.size() : 0;
            int subTreeCount = subTrees != null ? subTrees.size() : 0;

            int childTreeCount = subTreeBuildersCount + subTreeCount;
            if (childTreeCount == 0) {
                return Collections.emptyMap();
            }

            Map<ConfigKey, ConfigTree> result = CollectionsEx.newHashMap(childTreeCount);

            if (subTrees != null) {
                result.putAll(subTrees);
            }
            addFromTreeBuilders(result);

            return Collections.unmodifiableMap(result);
        }
    }

    private final String value;
    private final Map<ConfigKey, ConfigTree> subTrees;

    private ConfigTree(Builder builder) {
        this(builder.value, builder.getChildTreesSnapshot());
    }

    private ConfigTree(String value, Map<ConfigKey, ConfigTree> subTrees) {
        this.value = value;
        this.subTrees = subTrees;
    }

    public static ConfigTree singleValue(String value) {
        return value != null
                ? new ConfigTree(value, Collections.<ConfigKey, ConfigTree>emptyMap())
                : EMPTY;
    }

    public boolean hasValues() {
        // We don't store subtrees with no values at all.
        return value != null || !subTrees.isEmpty();
    }

    public String getValue(@Nullable String defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Nonnull
    public Map<ConfigKey, ConfigTree> getSubTrees() {
        return subTrees;
    }

    @Nonnull
    public ConfigTree getDeepSubTree(String... keyNames) {
        ExceptionHelper.checkNotNullElements(keyNames, "keyNames");

        ConfigTree result = this;
        for (String key: keyNames) {
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getSubTree(key);
        }
        return result;
    }

    @Nonnull
    public ConfigTree getSubTree(String keyName) {
        return getSubTree(new ConfigKey(keyName, null));
    }

    @Nonnull
    public ConfigTree getDeepSubTree(ConfigPath path) {
        ExceptionHelper.checkNotNullArgument(path, "path");

        return getDeepSubTree(path.getKeys().iterator());
    }

    @Nonnull
    public ConfigTree getDeepSubTree(ConfigKey... keys) {
        ExceptionHelper.checkNotNullElements(keys, "keys");

        ConfigTree result = this;
        for (ConfigKey key: keys) {
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getSubTree(key);
        }
        return result;
    }

    private ConfigTree getDeepSubTree(Iterator<ConfigKey> keys) {
        ConfigTree result = this;
        while (keys.hasNext()) {
            ConfigKey key = keys.next();
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getSubTree(key);
        }
        return result;
    }

    @Nonnull
    public ConfigTree getSubTree(ConfigKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        ConfigTree result = subTrees.get(key);
        return result != null ? result : EMPTY;
    }
}
