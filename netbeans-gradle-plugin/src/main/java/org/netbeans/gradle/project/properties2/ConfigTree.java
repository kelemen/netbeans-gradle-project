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
        private String value;
        private Map<ConfigKey, ConfigTree> childTrees;
        private Map<ConfigKey, ConfigTree.Builder> childTreeBuilders;
        private ConfigTree cachedBuilt;

        public Builder(@Nonnull ConfigTree initialValue) {
            this();

            value = initialValue.value;

            if (!initialValue.childTrees.isEmpty()) {
                getChildTrees().putAll(initialValue.childTrees);
            }
        }

        public Builder() {
            this.value = null;
            this.childTreeBuilders = null;
            this.childTrees = null;
            this.cachedBuilt = null;
        }

        private Map<ConfigKey, ConfigTree.Builder> getChildTreeBuilders() {
            Map<ConfigKey, ConfigTree.Builder> result = childTreeBuilders;
            if (result == null) {
                result = new HashMap<>();
                childTreeBuilders = result;
            }
            return result;
        }

        private Map<ConfigKey, ConfigTree> getChildTrees() {
            Map<ConfigKey, ConfigTree> result = childTrees;
            if (result == null) {
                result = new HashMap<>();
                childTrees = result;
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

            if (childTreeBuilders != null) {
                childTreeBuilders.remove(key);
            }

            cachedBuilt = null;
            getChildTrees().put(key, tree);
        }

        public Builder getDeepChildBuilder(@Nonnull String... keyNames) {
            Builder result = this;
            for (String keyName: keyNames) {
                result = result.getChildBuilder(keyName);
            }
            return result;
        }

        public Builder getChildBuilder(@Nonnull String keyName) {
            return getChildBuilder(new ConfigKey(keyName, null));
        }

        public Builder getDeepChildBuilder(@Nonnull ConfigPath path) {
            Builder result = this;
            for (ConfigKey key: path.getKeys()) {
                result = result.getChildBuilder(key);
            }
            return result;
        }

        public Builder getDeepChildBuilder(@Nonnull ConfigKey... keys) {
            Builder result = this;
            for (ConfigKey key: keys) {
                result = result.getChildBuilder(key);
            }
            return result;
        }

        public Builder getChildBuilder(@Nonnull ConfigKey key) {
            Map<ConfigKey, Builder> childBuilders = getChildTreeBuilders();
            Builder result = childBuilders.get(key);
            if (result == null) {
                cachedBuilt = null;

                ConfigTree currentTree = childTrees != null
                        ? childTrees.remove(key)
                        : null;

                result = currentTree != null
                        ? new Builder(currentTree)
                        : new Builder();
                childBuilders.put(key, result);
            }

            return result;
        }

        public void detachChildTreeBuilders() {
            if (childTreeBuilders == null || childTreeBuilders.isEmpty()) {
                return;
            }

            Map<ConfigKey, ConfigTree> currentChildTrees = getChildTrees();
            for (Map.Entry<ConfigKey, Builder> entry: childTreeBuilders.entrySet()) {
                ConfigKey key = entry.getKey();
                ConfigTree tree = entry.getValue().create();

                currentChildTrees.put(key, tree);
            }
            childTreeBuilders = null;

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
            if (childTreeBuilders == null) {
                return;
            }

            for (Map.Entry<ConfigKey, ConfigTree.Builder> entry: childTreeBuilders.entrySet()) {
                ConfigTree childTree = entry.getValue().create();
                if (childTree.hasValues()) {
                    result.put(entry.getKey(), childTree);
                }
            }
        }

        private Map<ConfigKey, ConfigTree> getChildTreesSnapshot() {
            int childTreeBuildersCount = childTreeBuilders != null ? childTreeBuilders.size() : 0;
            int childBuiltTreeCount = childTrees != null ? childTrees.size() : 0;

            int childTreeCount = childTreeBuildersCount + childBuiltTreeCount;
            if (childTreeCount == 0) {
                return Collections.emptyMap();
            }

            Map<ConfigKey, ConfigTree> result = CollectionsEx.newHashMap(childTreeCount);

            if (childTrees != null) {
                result.putAll(childTrees);
            }
            addFromTreeBuilders(result);

            return Collections.unmodifiableMap(result);
        }
    }

    private final String value;
    private final Map<ConfigKey, ConfigTree> childTrees;

    private ConfigTree(Builder builder) {
        this(builder.value, builder.getChildTreesSnapshot());
    }

    private ConfigTree(String value, Map<ConfigKey, ConfigTree> childTrees) {
        this.value = value;
        this.childTrees = childTrees;
    }

    public static ConfigTree singleValue(String value) {
        return value != null
                ? new ConfigTree(value, Collections.<ConfigKey, ConfigTree>emptyMap())
                : EMPTY;
    }

    public boolean hasValues() {
        // We don't store childtrees with no values at all.
        return value != null || !childTrees.isEmpty();
    }

    public String getValue(@Nullable String defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Nonnull
    public Map<ConfigKey, ConfigTree> getChildTrees() {
        return childTrees;
    }

    @Nonnull
    public ConfigTree getDeepChildTree(String... keyNames) {
        ExceptionHelper.checkNotNullElements(keyNames, "keyNames");

        ConfigTree result = this;
        for (String key: keyNames) {
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getChildTree(key);
        }
        return result;
    }

    @Nonnull
    public ConfigTree getChildTree(String keyName) {
        return getChildTree(new ConfigKey(keyName, null));
    }

    @Nonnull
    public ConfigTree getDeepChildTree(ConfigPath path) {
        ExceptionHelper.checkNotNullArgument(path, "path");

        return getDeepChildTree(path.getKeys().iterator());
    }

    @Nonnull
    public ConfigTree getDeepChildTree(ConfigKey... keys) {
        ExceptionHelper.checkNotNullElements(keys, "keys");

        ConfigTree result = this;
        for (ConfigKey key: keys) {
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getChildTree(key);
        }
        return result;
    }

    private ConfigTree getDeepChildTree(Iterator<ConfigKey> keys) {
        ConfigTree result = this;
        while (keys.hasNext()) {
            ConfigKey key = keys.next();
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getChildTree(key);
        }
        return result;
    }

    @Nonnull
    public ConfigTree getChildTree(ConfigKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        ConfigTree result = childTrees.get(key);
        return result != null ? result : EMPTY;
    }
}
