package org.netbeans.gradle.project.properties2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class ConfigTree {
    public static final ConfigTree EMPTY = new Builder().create();

    public static final class Builder {
        private final Map<ConfigKey, String> values;
        private final Map<ConfigKey, ConfigTree.Builder> subTrees;

        public Builder() {
            this.values = new HashMap<>();
            this.subTrees = new HashMap<>();
        }

        public void setValue(@Nonnull ConfigKey key, @Nonnull String value) {
            ExceptionHelper.checkNotNullArgument(key, "key");
            ExceptionHelper.checkNotNullArgument(value, "value");

            if (subTrees.containsKey(key)) {
                throw new IllegalStateException("Configuration tree contains a subtree with the given key: " + key);
            }

            values.put(key, value);
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

        public Builder getDeepSubBuilder(@Nonnull ConfigKey... keys) {
            Builder result = this;
            for (ConfigKey key: keys) {
                result = result.getSubBuilder(key);
            }
            return result;
        }

        public Builder getSubBuilder(@Nonnull ConfigKey key) {
            Builder result = subTrees.get(key);
            if (result == null) {
                if (values.containsKey(key)) {
                    throw new IllegalStateException("Configuration tree contains a value with the given key: " + key);
                }

                result = new Builder();
                subTrees.put(key, result);
            }
            return result;
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

        private Map<ConfigKey, ConfigTree> getChildTreesSnapshot() {
            int subTreeCount = subTrees.size();
            if (subTreeCount == 0) {
                return Collections.emptyMap();
            }

            Map<ConfigKey, ConfigTree> result = CollectionsEx.newHashMap(subTreeCount);

            for (Map.Entry<ConfigKey, ConfigTree.Builder> entry: subTrees.entrySet()) {
                ConfigTree subTree = entry.getValue().create();
                if (subTree.hasValues()) {
                    result.put(entry.getKey(), subTree);
                }
            }

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

    @Nonnull
    public ConfigTree getSubTree(ConfigKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        ConfigTree result = subTrees.get(key);
        return result != null ? result : EMPTY;
    }
}
