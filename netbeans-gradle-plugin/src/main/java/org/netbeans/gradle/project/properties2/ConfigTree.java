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
    public static final ConfigTree EMPTY = new Builder().create();

    public static final class Builder {
        private final Map<ConfigKey, String> values;
        private Map<ConfigKey, ConfigTree> subTrees;
        private Map<ConfigKey, ConfigTree.Builder> subTreeBuilders;

        public Builder(@Nonnull ConfigTree initialValue) {
            this();

            values.putAll(initialValue.values);

            if (!initialValue.subTrees.isEmpty()) {
                getSubTrees().putAll(initialValue.subTrees);
            }
        }

        public Builder() {
            this.values = new HashMap<>();
            this.subTreeBuilders = null;
            this.subTrees = null;
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

        public void setValue(@Nonnull String keyName, @Nonnull String value) {
            setValue(new ConfigKey(keyName, null), value);
        }

        public void setValue(@Nonnull ConfigKey key, @Nonnull String value) {
            ExceptionHelper.checkNotNullArgument(key, "key");
            ExceptionHelper.checkNotNullArgument(value, "value");

            values.put(key, value);
        }

        public void setChildTree(@Nonnull ConfigKey key, @Nonnull ConfigTree tree) {
            ExceptionHelper.checkNotNullArgument(key, "key");
            ExceptionHelper.checkNotNullArgument(tree, "tree");

            if (subTreeBuilders != null) {
                subTreeBuilders.remove(key);
            }

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
                ConfigTree currentTree = subTrees.remove(key);

                result = currentTree != null
                        ? new Builder(currentTree)
                        : new Builder();
                childBuilders.put(key, result);
            }

            return result;
        }

        private ConfigTree createDeepChild(Iterator<ConfigKey> keys) {
            Builder prev = null;
            ConfigKey prevKey = null;

            Builder result = this;
            while (keys.hasNext()) {
                ConfigKey key = keys.next();

                ConfigTree builtTree = result.subTrees != null
                        ? result.subTrees.get(key)
                        : null;
                if (builtTree != null) {
                    return builtTree.getDeepSubTree(keys);
                }

                prev = result;
                prevKey = key;
                result = result.subTreeBuilders != null
                        ? result.subTreeBuilders.get(key)
                        : null;
                if (result == null) {
                    return EMPTY;
                }
            }

            ConfigTree builtResult = result.create();
            if (prev != null) {
                prev.subTreeBuilders.remove(prevKey);
                prev.subTrees.put(prevKey, builtResult);
            }
            return builtResult;
        }

        public ConfigTree createDeepChild(ConfigPath path) {
            ExceptionHelper.checkNotNullArgument(path, "path");
            return createDeepChild(path.getKeys().iterator());
        }

        public ConfigTree create() {
            return new ConfigTree(this);
        }

        private Map<ConfigKey, String> getValuesSnapshot() {
            if (values.isEmpty()) {
                return Collections.emptyMap();
            }

            return Collections.unmodifiableMap(new HashMap<>(values));
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

    private final Map<ConfigKey, String> values;
    private final Map<ConfigKey, ConfigTree> subTrees;

    private ConfigTree(Builder builder) {
        this.values = builder.getValuesSnapshot();
        this.subTrees = builder.getChildTreesSnapshot();
    }

    public boolean hasValues() {
        // We don't store subtrees with no values at all.
        return !values.isEmpty() || !subTrees.isEmpty();
    }

    @Nonnull
    public Map<ConfigKey, String> getValues() {
        return values;
    }

    @Nonnull
    public Map<ConfigKey, ConfigTree> getSubTrees() {
        return subTrees;
    }

    @Nullable
    public String tryGetValue(String keyName) {
        return values.get(new ConfigKey(keyName, null));
    }

    @Nullable
    public String tryGetValue(ConfigKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        return values.get(key);
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
