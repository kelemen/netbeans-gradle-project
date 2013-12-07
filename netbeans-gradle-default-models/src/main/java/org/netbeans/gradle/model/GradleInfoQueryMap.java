package org.netbeans.gradle.model;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.gradle.model.api.GradleInfoQuery;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;
import org.netbeans.gradle.model.api.ModelClassPathDef;
import org.netbeans.gradle.model.internal.CustomSerializedMap;
import org.netbeans.gradle.model.internal.IssueTransformer;
import org.netbeans.gradle.model.internal.SerializedEntries;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;

final class GradleInfoQueryMap {
    private final CustomSerializedMap builderMap;
    private final Map<KeyWrapper, ModelClassPathDef> classpath;
    private final Map<KeyWrapper, Throwable> serializationIssues;

    private GradleInfoQueryMap(
            CustomSerializedMap builderMap,
            Map<KeyWrapper, ModelClassPathDef> classpath,
            Map<Object, Throwable> serializationIssues) {
        this.builderMap = builderMap;
        this.classpath = classpath;

        if (serializationIssues.isEmpty()) {
            this.serializationIssues = Collections.emptyMap();
        }
        else {
            Map<KeyWrapper, Throwable> issues = CollectionUtils.newHashMap(serializationIssues.size());
            for (Map.Entry<Object, Throwable> entry: serializationIssues.entrySet()) {
                issues.put((KeyWrapper)entry.getKey(), entry.getValue());
            }

            this.serializationIssues = issues;
        }
    }

    private static <QueryType extends GradleInfoQuery> GradleInfoQueryMap fromQueries(
            Map<Object, List<QueryType>> map,
            BuilderRetriever<QueryType> builderRetriever) {

        Map<KeyWrapper, ModelClassPathDef> classpath = new HashMap<KeyWrapper, ModelClassPathDef>(32);

        CustomSerializedMap.Builder builders = new CustomSerializedMap.Builder(map.size());
        for (Map.Entry<?, List<QueryType>> entry: map.entrySet()) {
            Object entryKey = entry.getKey();

            int index = 0;
            for (QueryType query: entry.getValue()) {
                KeyWrapper key = new KeyWrapper(index, entryKey);

                builders.addValue(key, builderRetriever.getBuilder(query));
                classpath.put(key, query.getInfoClassPath());

                index++;
            }
        }

        Map<Object, Throwable> serializationIssues = new HashMap<Object, Throwable>();
        CustomSerializedMap serializedBuilders = builders.create(serializationIssues);

        return new GradleInfoQueryMap(
                serializedBuilders,
                classpath,
                serializationIssues);
    }

    public static GradleInfoQueryMap fromBuildInfos(Map<Object, List<GradleBuildInfoQuery<?>>> map) {
        return fromQueries(map, new BuilderRetriever<GradleBuildInfoQuery<?>>() {
            public Object getBuilder(GradleBuildInfoQuery<?> infoQuery) {
                return infoQuery.getInfoBuilder();
            }
        });
    }

    public static GradleInfoQueryMap fromProjectInfos(Map<Object, List<GradleProjectInfoQuery<?>>> map) {
        return fromQueries(map, new BuilderRetriever<GradleProjectInfoQuery<?>>() {
            public Object getBuilder(GradleProjectInfoQuery<?> infoQuery) {
                return infoQuery.getInfoBuilder();
            }
        });
    }

    private ClassLoader getClassLoaderForKey(KeyWrapper key) {
        ModelClassPathDef classpathDef = classpath.get(key);
        return classpathDef != null
                ? classpathDef.getClassLoader()
                : null;
    }

    public CustomSerializedMap getBuilderMap() {
        return builderMap;
    }

    public CustomSerializedMap.Deserializer getSerializableBuilderMap() {
        return new Deserializer(builderMap, classpath);
    }

    private static void addToMultiMap(Object key, Object value, Map<Object, List<Object>> result) {
        addAllToMultiMap(key, Collections.singletonList(value), result);
    }

    private static void addAllToMultiMap(Object key, List<?> values, Map<Object, List<Object>> result) {
        List<Object> container = result.get(key);
        if (container == null) {
            container = new LinkedList<Object>();
            result.put(key, container);
        }
        container.addAll(values);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, List<?>> unsafeCast(Map<Object, List<Object>> map) {
        return (Map<Object, List<?>>)(Map<?, ?>)map;
    }

    public Map<Object, List<?>> deserializeResults(
            CustomSerializedMap map,
            IssueTransformer issueTransformer) {

        if (map == null) throw new NullPointerException("map");
        if (issueTransformer == null) throw new NullPointerException("issueTransformer");

        Map<Object, List<Object>> result = CollectionUtils.newHashMap(map.size());

        for (Map.Entry<Object, SerializedEntries> entry: map.getMap().entrySet()) {
            KeyWrapper key = (KeyWrapper)entry.getKey();
            List<?> values = entry.getValue().getUnserialized(getClassLoaderForKey(key));
            addAllToMultiMap(key.wrappedKey, values, result);
        }

        for (Map.Entry<Object, Throwable> entry: map.getSerializationProblems().entrySet()) {
            KeyWrapper key = (KeyWrapper)entry.getKey();
            Object issue = issueTransformer.transformIssue(entry.getValue());
            addToMultiMap(key.wrappedKey, issue, result);
        }

        for (Map.Entry<KeyWrapper, Throwable> entry: serializationIssues.entrySet()) {
            Object key = entry.getKey().wrappedKey;
            Object issue = issueTransformer.transformIssue(entry.getValue());
            addToMultiMap(key, issue, result);
        }

        return unsafeCast(result);
    }

    public static IssueTransformer builderIssueTransformer() {
        return BuilderIssueTransformer.INSTANCE;
    }

    private static enum BuilderIssueTransformer implements IssueTransformer {
        INSTANCE;

        public Object transformIssue(Throwable issue) {
            return new BuilderResult(null, new BuilderIssue("", issue));
        }
    }

    private static final class Deserializer
    implements
            CustomSerializedMap.Deserializer, Serializable {

        private static final long serialVersionUID = 1L;

        private final CustomSerializedMap builderMap;
        private final Map<KeyWrapper, Set<File>> paths;

        public Deserializer(
                CustomSerializedMap builderMap,
                Map<KeyWrapper, ModelClassPathDef> classpath) {
            if (builderMap == null) throw new NullPointerException("builderMap");

            this.builderMap = builderMap;
            this.paths = CollectionUtils.newHashMap(classpath.size());
            for (Map.Entry<KeyWrapper, ModelClassPathDef> entry: classpath.entrySet()) {
                KeyWrapper key = entry.getKey();
                if (key == null) throw new NullPointerException("classpath[?].key");

                this.paths.put(key, entry.getValue().getJarFiles());
            }
        }

        private ClassLoader getClassLoaderForKey(
                KeyWrapper key,
                ClassLoader parent,
                Map<Set<File>, ClassLoader> cache) {

            Set<File> files = paths.get(key);
            if (files == null || files.isEmpty()) {
                return parent;
            }

            ClassLoader result = cache.get(files);
            if (result != null) {
                return result;
            }

            result = ClassLoaderUtils.classLoaderFromClassPath(files, parent);
            cache.put(files, result);
            return result;
        }

        public Map<Object, List<?>> deserialize(ClassLoader parent) {
            Map<Set<File>, ClassLoader> cache = new HashMap<Set<File>, ClassLoader>();
            Map<Object, List<?>> result = CollectionUtils.newHashMap(builderMap.size());

            for (Map.Entry<Object, SerializedEntries> entry: builderMap.getMap().entrySet()) {
                KeyWrapper key = (KeyWrapper)entry.getKey();

                ClassLoader classLoader = getClassLoaderForKey(key, parent, cache);
                result.put(key, entry.getValue().getUnserialized(classLoader));
            }

            return result;
        }
    }

    private static interface BuilderRetriever<QueryType extends GradleInfoQuery> {
        public Object getBuilder(QueryType infoQuery);
    }

    private static final class KeyWrapper implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int index;
        public final Object wrappedKey;

        public KeyWrapper(int index, Object key) {
            this.index = index;
            this.wrappedKey = key;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + this.index;
            hash = 59 * hash + (this.wrappedKey != null ? this.wrappedKey.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final KeyWrapper other = (KeyWrapper)obj;
            if (this.index != other.index) return false;

            return this.wrappedKey == other.wrappedKey || (this.wrappedKey != null && this.wrappedKey.equals(other.wrappedKey));
        }
    }
}
