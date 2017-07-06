package org.netbeans.gradle.project.api.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines a tree based configuration store. The edges in the tree are identified
 * by a string and also each node may store a string value (there is no
 * restriction on what characters the strings may contain). In most practical
 * use-cases, you can think of edges as {@literal XML} tags and values as their
 * {@literal CDATA} content. All strings are treated as case sensitive.
 * <P>
 * It is possible to have multiple edges with the same name with the given
 * parent, in this case all the subtrees can be requested as a list
 * (order is maintained).
 * <P>
 * Instances of {@code ConfigTree} can be created through its {@link Builder}.
 * <P>
 * Instances of this class are immutable and as such can be shared without any
 * further synchronization.
 */
public final class ConfigTree {
    private static final int DEFAULT_LIST_SIZE = 10;

    /**
     * Defines an empty tree with a single node with no value.
     */
    public static final ConfigTree EMPTY = new Builder().create();

    /**
     * Defines a builder to create {@code ConfigTree} instances.
     */
    public static final class Builder {
        private String value;
        private Map<String, List<TreeOrBuilder>> childTrees;
        private ConfigTree cachedBuilt;

        /**
         * Creates a new {@code Builder} with the given configuration tree as
         * its initial value. That is, if you call {@link #create() create} right
         * after creating the new {@code Builder}, you will get effectively the
         * same {@code ConfigTree} what was specified in the argument.
         *
         * @param initialValue the base configuration tree of this {@code Builder}.
         *   This argument cannot be {@code null}.
         */
        public Builder(@Nonnull ConfigTree initialValue) {
            this();

            value = initialValue.value;

            if (!initialValue.childTrees.isEmpty()) {
                Map<String, List<TreeOrBuilder>> children
                        = CollectionsEx.newHashMap(initialValue.childTrees.size());
                childTrees = children;

                for (Map.Entry<String, List<ConfigTree>> entry: initialValue.childTrees.entrySet()) {
                    List<ConfigTree> entryValue = entry.getValue();

                    List<TreeOrBuilder> valueList = createList(entryValue.size());
                    children.put(entry.getKey(), valueList);

                    for (ConfigTree child: entryValue) {
                        valueList.add(new TreeOrBuilder(child));
                    }
                }
            }
        }

        /**
         * Creates a new {@code Builder} with only a single root node without
         * any value.
         */
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

        private List<TreeOrBuilder> getChildTreeList(String key) {
            return getList(getChildTrees(), key);
        }

        private Map<String, List<TreeOrBuilder>> getChildTrees() {
            Map<String, List<TreeOrBuilder>> result = childTrees;
            if (result == null) {
                result = new HashMap<>();
                childTrees = result;
            }
            return result;
        }

        /**
         * Sets the string value associated with the root node. Note that,
         * usually it is not recommended (though it is possible) for non-leaf
         * nodes to have a value.
         *
         * @param value the value of the root node. This argument can be
         *   {@code null}, meaning that this node has no value.
         */
        public void setValue(@Nullable String value) {
            cachedBuilt = null;
            this.value = value;
        }

        /**
         * Connects the root node of this {@code Builder} with the specied
         * configuration tree with the given edge.
         * <P>
         * <B>Note</B>: This method will effectievly detach all subtrees along
         * the given edge. So builders previously created with the given edge
         * will no longer affect this builder.
         *
         * @param key the string identifying the edge connecting the given
         *   subtree with the root. This argument cannot be {@code null}.
         * @param tree the subtree to be connected with the root node. This
         *   argument cannot be {@code null}.
         */
        public void setChildTree(@Nonnull String key, @Nonnull ConfigTree tree) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(tree, "tree");

            cachedBuilt = null;

            List<TreeOrBuilder> valueList = getEmptyList(getChildTrees(), key);
            valueList.add(new TreeOrBuilder(tree));
        }

        /**
         * Returns a {@code Builder} which can be used to build subtree of this
         * {@code Builder}. Non-existent subtrees in the path will be created,
         * however note that already created subtree builders will be reused
         * (will not add new subtrees).
         * <P>
         * <B>Note</B>: The returned {@code Builder} will keep affecting
         * its parent {@code Builder} instances as long as
         * {@link #detachChildTreeBuilders() detachChildTreeBuilders} is not
         * called. Once {@code detachChildTreeBuilders} gets called on a tree,
         * none of its previously created subtree builders can affect that parent.
         * <P>
         * If you need multiple subtrees connected with an edge with the same
         * name, you have to use the {@link #addChildBuilder(String) addChildBuilder}
         * method instead.
         * <P>
         * This method is effectively the same as repeatedly calling
         * {@link #getChildBuilder(String) getChildBuilder} on each returned
         * {@code Builder} with the elements of the given array.
         *
         * @param keyNames the names of the edges identifying the subtree to
         *   be edited. This argument cannot be {@code null} and neither of its
         *   elements can be {@code null}.
         * @return the {@code Builder} which can be used to edit the given
         *   subtree. This method never returns {@code null}.
         */
        @Nonnull
        public Builder getDeepChildBuilder(@Nonnull String... keyNames) {
            Builder result = this;
            for (String keyName: keyNames) {
                result = result.getChildBuilder(keyName);
            }
            return result;
        }

        /**
         * Returns a {@code Builder} which can be used to build subtree of this
         * {@code Builder}. Non-existent subtrees in the path will be created,
         * however note that already created subtree builders will be reused
         * (will not add new subtrees).
         * <P>
         * <B>Note</B>: The returned {@code Builder} will keep affecting
         * its parent {@code Builder} instances as long as
         * {@link #detachChildTreeBuilders() detachChildTreeBuilders} is not
         * called. Once {@code detachChildTreeBuilders} gets called on a tree,
         * none of its previously created subtree builders can affect that parent.
         * <P>
         * If you need multiple subtrees connected with an edge with the same
         * name, you have to use the {@link #addChildBuilder(String) addChildBuilder}
         * method instead.
         * <P>
         * This method is effectively the same as repeatedly calling
         * {@link #getChildBuilder(String) getChildBuilder} on each returned
         * {@code Builder} with the keys of the given {@code ConfigPath}.
         *
         * @param path the path identifying the subtree to be edited. This
         *   argument cannot be {@code null}.
         * @return the {@code Builder} which can be used to edit the given
         *   subtree. This method never returns {@code null}.
         */
        @Nonnull
        public Builder getDeepChildBuilder(@Nonnull ConfigPath path) {
            Builder result = this;
            for (String key: path.getKeys()) {
                result = result.getChildBuilder(key);
            }
            return result;
        }

        /**
         * Returns a {@code Builder} which can be used to build subtree of this
         * {@code Builder}. If a subtree identified by the given edge does not
         * exist it will be created. If it already exist, the first builder
         * with the given name will be returned.
         * <P>
         * <B>Note</B>: The returned {@code Builder} will keep affecting
         * its parent {@code Builder} instances as long as
         * {@link #detachChildTreeBuilders() detachChildTreeBuilders} is not
         * called. Once {@code detachChildTreeBuilders} gets called on a tree,
         * none of its previously created subtree builders can affect that parent.
         * <P>
         * If you need multiple subtrees connected with an edge with the same
         * name, you have to use the {@link #addChildBuilder(String) addChildBuilder}
         * method instead.
         *
         * @param key the key identifying the subtree to be edited. This
         *   argument cannnot be {@code null}.
         * @return the {@code Builder} which can be used to edit the given
         *   subtree. This method never returns {@code null}.
         */
        @Nonnull
        public Builder getChildBuilder(@Nonnull String key) {
            Objects.requireNonNull(key, "key");

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

        /**
         * Adds a subtree builder connected to the root node with the given name.
         * If there is no subtree connected with this name, a new one is created.
         * If there is already a subtree connected with this name, a new one
         * is added with the same name.
         * <P>
         * <B>Note</B>: {@code getChildBuilder} and similar methods will only
         * use the first child added and will ignore subsequently added subtrees
         * with the same name.
         *
         * @param key the key identifying the subtree to be edited. This
         *   argument cannnot be {@code null}.
         * @return the {@code Builder} which can be used to edit the given
         *   subtree. This method never returns {@code null}.
         */
        @Nonnull
        public Builder addChildBuilder(@Nonnull String key) {
            Objects.requireNonNull(key, "key");

            cachedBuilt = null;

            Builder result = new Builder();
            getChildTreeList(key).add(new TreeOrBuilder(result));
            return result;
        }

        /**
         * Remove all subtrees connected to the root node with an edge with the
         * given name. Further editing child subtrees connected with this name
         * will no longer affect this builder.
         *
         * @param key the string identifying the edge connecting the subtrees
         *   to be removed. This argument cannot be {@code null}.
         */
        public void removeChild(@Nonnull String key) {
            Objects.requireNonNull(key, "key");

            if (childTrees == null) {
                return;
            }

            if (childTrees.remove(key) != null) {
                cachedBuilt = null;
            }
        }

        /**
         * Makes so the previously created subtree builders no longer affect
         * this {@code Builder}. Note that this method does not remove child
         * trees, simply makes so that subsequent edits to <I>previously</I>
         * created builders can no longer affect this builder.
         */
        public void detachChildTreeBuilders() {
            if (childTrees == null || childTrees.isEmpty()) {
                return;
            }

            Map<String, List<TreeOrBuilder>> children = childTrees;
            for (List<TreeOrBuilder> valueList: children.values()) {
                for (TreeOrBuilder child: valueList) {
                    child.makeTree();
                }
            }

            cachedBuilt = create();
        }

        /**
         * Creates a new immutable snapshot of the configuration tree built
         * by this builder.
         *
         * @return a new immutable snapshot of the configuration tree built
         *   by this builder. This method never returns {@code null}.
         */
        @Nonnull
        public ConfigTree create() {
            ConfigTree result = cachedBuilt;
            if (result == null) {
                result = new ConfigTree(this);
            }
            return result;
        }

        private Map<String, List<ConfigTree>> getChildTreesSnapshot() {
            Map<String, List<TreeOrBuilder>> children = childTrees;
            int childTreeCount = children != null ? children.size() : 0;
            if (childTreeCount == 0) {
                return Collections.emptyMap();
            }

            Map<String, List<ConfigTree>> result = CollectionsEx.newHashMap(childTreeCount);

            if (children != null) {
                for (Map.Entry<String, List<TreeOrBuilder>> entry: children.entrySet()) {
                    String entryKey = entry.getKey();
                    List<TreeOrBuilder> entryValue = entry.getValue();
                    int entryValueSize = entryValue.size();

                    if (entryValueSize == 1) {
                        // A common special case
                        ConfigTree child = entryValue.get(0).createTreeIfHasValue();
                        if (child != null) {
                            result.put(entryKey, Collections.singletonList(child));
                        }
                    }
                    else if (entryValueSize > 0) {
                        List<ConfigTree> resultChild = new ArrayList<>(entryValueSize);
                        for (TreeOrBuilder child: entryValue) {
                            ConfigTree builtTree = child.createTreeIfHasValue();
                            if (builtTree != null) {
                                resultChild.add(builtTree);
                            }
                        }
                        result.put(entryKey, Collections.unmodifiableList(resultChild));
                    }
                }
            }

            return Collections.unmodifiableMap(result);
        }
    }

    private final String value;
    private final Map<String, List<ConfigTree>> childTrees;
    private int hash; // Only works if we rely on the default value: 0

    private ConfigTree(Builder builder) {
        this(builder.value, builder.getChildTreesSnapshot());
    }

    private ConfigTree(String value, Map<String, List<ConfigTree>> childTrees) {
        this.value = value;
        this.childTrees = childTrees;
    }

    /**
     * Creates a configuration tree with only a single node with the given
     * value. If the specified value is {@code null}, the return value is the
     * {@link #EMPTY empty tree}.
     *
     * @param value the value of the root (and only) node of the returned
     *   configuration tree.
     * @return a configuration tree with only a single node with the given
     *   value. This method never returns {@code null}.
     */
    @Nonnull
    public static ConfigTree singleValue(@Nullable String value) {
        return value != null
                ? new ConfigTree(value, Collections.<String, List<ConfigTree>>emptyMap())
                : EMPTY;
    }

    /**
     * Checks whether this node has any child or value at all. That is, this
     * method returns {@code true}, if and only, if this configuration tree
     * is equivalent of the {@link #EMPTY empty tree}.
     *
     * @return {@code true} if this node is equivalent of the {@link #EMPTY empty tree},
     *   {@code false} otherwise
     */
    public boolean hasValues() {
        // We don't store childtrees with no values at all.
        return value != null || !childTrees.isEmpty();
    }

    /**
     * Returns the value of this node or the speficied default value if this
     * node has no associate value.
     *
     * @param defaultValue the value to return if the root node has no associated
     *   value. This argument can be {@code null}.
     * @return the value of this node or the speficied default value if this
     *   node has no associate value. Notice that this method may only return
     *   {@code null}, if the specified default value is {@code null}.
     */
    public String getValue(@Nullable String defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Returns all the child trees as a map where keys are the name of the
     * edges connecting the subtrees (values).
     * <P>
     * This method returns a normalized value in such way that none of the
     * returned {@code ConfigTree} instances are equivalent to the
     * {@link #EMPTY empty tree}.
     *
     * @return all the child trees as a map where keys are the name of the
     *   edges connecting the subtrees (values). This method never returns
     *   {@code null}.
     */
    @Nonnull
    public Map<String, List<ConfigTree>> getChildTrees() {
        return childTrees;
    }

    /**
     * Returns the subtree along the given path. This method never fails, if
     * this configuration tree does not contain any subtree along this path,
     * this method returns an empty tree.
     * <P>
     * If the given path contains no keys, this {@code ConfigTree} is returned.
     *
     * @param path the {@code ConfigPath} identifying the subtree to be
     *   returned. This argument cannot be {@code null}.
     * @return the subtree along the given path. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public ConfigTree getDeepChildTree(@Nonnull ConfigPath path) {
        Objects.requireNonNull(path, "path");
        return getDeepChildTree(path.getKeys().iterator());
    }

     /**
     * Returns the subtree along the given path. This method never fails, if
     * this configuration tree does not contain any subtree along this path,
     * this method returns an empty tree.
     * <P>
     * If the given path contains no keys, this {@code ConfigTree} is returned.
     *
     * @param keys the name of edges identifying the subtree to be
     *   returned. This argument cannot be {@code null} and the array cannot
     *   contain {@code null} elements.
     * @return the subtree along the given path. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public ConfigTree getDeepChildTree(@Nonnull String... keys) {
        ExceptionHelper.checkNotNullElements(keys, "keys");

        ConfigTree result = this;
        for (String key: keys) {
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getChildTree(key);
        }
        return result;
    }

    private ConfigTree getDeepChildTree(Iterator<String> keys) {
        ConfigTree result = this;
        while (keys.hasNext()) {
            String key = keys.next();
            // Minor optimization
            if (result == EMPTY) {
                return EMPTY;
            }

            result = result.getChildTree(key);
        }
        return result;
    }

    /**
     * Returns all the child trees connected with an edge with the given name.
     * If there are no such subtrees, an empty list is returned.
     * <P>
     * This method returns a normalized value in such way that none of the
     * returned {@code ConfigTree} instances are equivalent to the
     * {@link #EMPTY empty tree}.
     *
     * @param key the name of the edge connecting the subtrees to be returned.
     *   This argument cannot be {@code null}.
     * @return all the child trees connected with an edge with the given name.
     *   This method never returns {@code null} and none of the elements of
     *   list are {@code null}.
     */
    @Nonnull
    public List<ConfigTree> getChildTrees(@Nonnull String key) {
        Objects.requireNonNull(key, "key");

        List<ConfigTree> result = childTrees.get(key);
        return result != null ? result : Collections.<ConfigTree>emptyList();
    }

    /**
     * Returns the first child tree connected with an edge with the given name.
     * If there are no such subtree, an {@link #EMPTY empty tree} is returned.
     *
     * @param key the name of the edge connecting the subtree to be returned.
     *   This argument cannot be {@code null}.
     * @return the first child tree connected with an edge with the given name.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public ConfigTree getChildTree(@Nonnull String key) {
        Objects.requireNonNull(key, "key");

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

        public ConfigTree createTreeIfHasValue() {
            ConfigTree result = createTree();
            return result.hasValues() ? result : null;
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
        int result = hash;
        if (result == 0) {
            result = 3;
            result = 83 * result + Objects.hashCode(value);
            result = 83 * result + Objects.hashCode(childTrees);
            hash = result;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ConfigTree other = (ConfigTree)obj;
        return Objects.equals(this.value, other.value)
                && Objects.equals(this.childTrees, other.childTrees);
    }

    private static void appendIndent(StringBuilder result, int indent) {
        final String indentStr = "  ";
        for (int i = 0; i < indent; i++) {
            result.append(indentStr);
        }
    }

    private void toString(String prefix, int indent, StringBuilder result) {
        appendIndent(result, indent);
        result.append(prefix);
        result.append("ConfigTree");
        if (value != null) {
            result.append(" (");
            result.append(value);
            result.append(")");
        }

        for (Map.Entry<String, List<ConfigTree>> entry: childTrees.entrySet()) {
            String key = entry.getKey();
            for (ConfigTree tree: entry.getValue()) {
                result.append('\n');
                tree.toString(key + " -> ", indent + 1, result);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        toString("", 0, result);
        return result.toString();
    }
}
