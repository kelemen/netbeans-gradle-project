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
        private Map<ConfigKey, TreeOrBuilder> childTrees;
        private ConfigTree cachedBuilt;

        public Builder(@Nonnull ConfigTree initialValue) {
            this();

            value = initialValue.value;

            if (!initialValue.childTrees.isEmpty()) {
                Map<ConfigKey, TreeOrBuilder> children
                        = CollectionsEx.newHashMap(initialValue.childTrees.size());
                childTrees = children;

                for (Map.Entry<ConfigKey, ConfigTree> entry: initialValue.childTrees.entrySet()) {
                    children.put(entry.getKey(), new TreeOrBuilder(entry.getValue()));
                }
            }
        }

        public Builder() {
            this.value = null;
            this.childTrees = null;
            this.cachedBuilt = null;
        }

        private Map<ConfigKey, TreeOrBuilder> getChildTrees() {
            Map<ConfigKey, TreeOrBuilder> result = childTrees;
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

            cachedBuilt = null;
            getChildTrees().put(key, new TreeOrBuilder(tree));
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
            Map<ConfigKey, TreeOrBuilder> children = getChildTrees();
            TreeOrBuilder result = children.get(key);
            if (result == null) {
                cachedBuilt = null;

                Builder builder = new Builder();
                children.put(key, new TreeOrBuilder(builder));
                return builder;
            }
            else {
                return result.getBuilder();
            }
        }

        public void detachChildTreeBuilders() {
            if (childTrees == null || childTrees.isEmpty()) {
                return;
            }

            Map<ConfigKey, TreeOrBuilder> children = getChildTrees();
            for (TreeOrBuilder child: children.values()) {
                child.makeTree();
            }

            cachedBuilt = create();
        }

        public ConfigTree create() {
            ConfigTree result = cachedBuilt;
            if (result == null) {
                result = new ConfigTree(this);
            }
            return result;
        }

        private Map<ConfigKey, ConfigTree> getChildTreesSnapshot() {
            Map<ConfigKey, TreeOrBuilder> children = childTrees;
            int childTreeCount = children != null ? children.size() : 0;
            if (childTreeCount == 0) {
                return Collections.emptyMap();
            }

            Map<ConfigKey, ConfigTree> result = CollectionsEx.newHashMap(childTreeCount);

            if (children != null) {
                for (Map.Entry<ConfigKey, TreeOrBuilder> entry: children.entrySet()) {
                    result.put(entry.getKey(), entry.getValue().createTree());
                }
            }

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

    private static final class TreeOrBuilder {
        private ConfigTree tree;
        private Builder builder;

        public TreeOrBuilder(ConfigTree tree) {
            assert tree != null;

            this.tree = tree;
            this.builder = null;
        }

        public TreeOrBuilder(Builder builder) {
            assert builder != null;

            this.tree = null;
            this.builder = builder;
        }

        public ConfigTree createTree() {
            ConfigTree result = tree;
            if (result == null) {
                result = builder.create();
            }
            return result;
        }

        public void makeTree() {
            if (builder != null) {
                tree = builder.create();
                builder = null;
            }
        }

        public Builder getBuilder() {
            Builder result = builder;
            if (result == null) {
                result = new Builder(tree);
                builder = result;
                tree = null;
            }
            return result;
        }
    }
}
