package org.netbeans.gradle.project.properties2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class ConfigTree {
    private static final int DEFAULT_LIST_SIZE = 10;

    public static final ConfigTree EMPTY = new Builder().create();

    public static final class Builder {
        private String value;
        private Map<ConfigKey, List<TreeOrBuilder>> childTrees;
        private ConfigTree cachedBuilt;

        public Builder(@Nonnull ConfigTree initialValue) {
            this();

            value = initialValue.value;

            if (!initialValue.childTrees.isEmpty()) {
                Map<ConfigKey, List<TreeOrBuilder>> children
                        = CollectionsEx.newHashMap(initialValue.childTrees.size());
                childTrees = children;

                for (Map.Entry<ConfigKey, List<ConfigTree>> entry: initialValue.childTrees.entrySet()) {
                    List<ConfigTree> entryValue = entry.getValue();

                    List<TreeOrBuilder> valueList = createList(entryValue.size());
                    children.put(entry.getKey(), valueList);

                    for (ConfigTree child: entryValue) {
                        valueList.add(new TreeOrBuilder(child));
                    }
                }
            }
        }

        public Builder() {
            this.value = null;
            this.childTrees = null;
            this.cachedBuilt = null;
        }

        private static <E> List<E> createList() {
            return createList(DEFAULT_LIST_SIZE);
        }

        private static <E> List<E> createList(int expectedSize) {
            return new ArrayList<>(Math.max(expectedSize, DEFAULT_LIST_SIZE));
        }

        private static <K, E> List<E> getList(Map<K, List<E>> map, K key) {
            List<E> result = map.get(key);
            if (result == null) {
                result = createList();
                map.put(key, result);
            }
            return result;
        }

        private static <K, E> List<E> getEmptyList(Map<K, List<E>> map, K key) {
            List<E> result = createList();
            map.put(key, result);
            return result;
        }

        private List<TreeOrBuilder> getChildTreeList(ConfigKey key) {
            return getList(getChildTrees(), key);
        }

        private Map<ConfigKey, List<TreeOrBuilder>> getChildTrees() {
            Map<ConfigKey, List<TreeOrBuilder>> result = childTrees;
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

            List<TreeOrBuilder> valueList = getEmptyList(getChildTrees(), key);
            valueList.add(new TreeOrBuilder(tree));
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
            ExceptionHelper.checkNotNullArgument(key, "key");

            List<TreeOrBuilder> valueList = getChildTreeList(key);
            if (valueList.isEmpty()) {
                cachedBuilt = null;

                Builder result = new Builder();
                valueList.add(new TreeOrBuilder(result));
                return result;
            }
            else {
                return valueList.get(0).getBuilder();
            }
        }

        public Builder addChildBuilder(@Nonnull String keyName) {
            return addChildBuilder(new ConfigKey(keyName, null));
        }

        public Builder addChildBuilder(@Nonnull ConfigKey key) {
            ExceptionHelper.checkNotNullArgument(key, "key");

            cachedBuilt = null;

            Builder result = new Builder();
            getChildTreeList(key).add(new TreeOrBuilder(result));
            return result;
        }

        public void detachChildTreeBuilders() {
            if (childTrees == null || childTrees.isEmpty()) {
                return;
            }

            Map<ConfigKey, List<TreeOrBuilder>> children = childTrees;
            for (List<TreeOrBuilder> valueList: children.values()) {
                for (TreeOrBuilder child: valueList) {
                    child.makeTree();
                }
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

        private Map<ConfigKey, List<ConfigTree>> getChildTreesSnapshot() {
            Map<ConfigKey, List<TreeOrBuilder>> children = childTrees;
            int childTreeCount = children != null ? children.size() : 0;
            if (childTreeCount == 0) {
                return Collections.emptyMap();
            }

            Map<ConfigKey, List<ConfigTree>> result = CollectionsEx.newHashMap(childTreeCount);

            if (children != null) {
                for (Map.Entry<ConfigKey, List<TreeOrBuilder>> entry: children.entrySet()) {
                    ConfigKey entryKey = entry.getKey();
                    List<TreeOrBuilder> entryValue = entry.getValue();
                    int entryValueSize = entryValue.size();

                    if (entryValueSize == 1) {
                        // A common special case
                        ConfigTree child = entryValue.get(0).createTree();
                        result.put(entryKey, Collections.singletonList(child));
                    }
                    else if (entryValueSize > 0) {
                        List<ConfigTree> resultChild = new ArrayList<>(entryValueSize);
                        for (TreeOrBuilder child: entryValue) {
                            resultChild.add(child.createTree());
                        }
                        result.put(entryKey, Collections.unmodifiableList(resultChild));
                    }
                }
            }

            return Collections.unmodifiableMap(result);
        }
    }

    private final String value;
    private final Map<ConfigKey, List<ConfigTree>> childTrees;

    private ConfigTree(Builder builder) {
        this(builder.value, builder.getChildTreesSnapshot());
    }

    private ConfigTree(String value, Map<ConfigKey, List<ConfigTree>> childTrees) {
        this.value = value;
        this.childTrees = childTrees;
    }

    public static ConfigTree singleValue(String value) {
        return value != null
                ? new ConfigTree(value, Collections.<ConfigKey, List<ConfigTree>>emptyMap())
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
    public Map<ConfigKey, List<ConfigTree>> getChildTrees() {
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
    public List<ConfigTree> getChildTrees(ConfigKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        List<ConfigTree> result = childTrees.get(key);
        return result != null ? result : Collections.<ConfigTree>emptyList();
    }

    @Nonnull
    public ConfigTree getChildTree(ConfigKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        List<ConfigTree> result = childTrees.get(key);
        return result != null ? result.get(0) : EMPTY;
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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(value);
        hash = 83 * hash + Objects.hashCode(childTrees);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ConfigTree other = (ConfigTree)obj;
        return Objects.equals(this.childTrees, other.childTrees);
    }
}
