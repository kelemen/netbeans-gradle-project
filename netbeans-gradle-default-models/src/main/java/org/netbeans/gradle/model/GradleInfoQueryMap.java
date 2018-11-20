package org.netbeans.gradle.model;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.BuildController;
import org.netbeans.gradle.model.api.GradleInfoQuery;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery2;
import org.netbeans.gradle.model.api.ModelClassPathDef;
import org.netbeans.gradle.model.internal.CustomSerializedMap;
import org.netbeans.gradle.model.internal.IssueTransformer;
import org.netbeans.gradle.model.internal.SerializedEntries;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.MultiMapUtils;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.SerializationCaches;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

final class GradleInfoQueryMap {
    private final CustomSerializedMap builderMap;
    private final Map<KeyWrapper, ModelClassPathDef> classpath;
    private final Map<KeyWrapper, Throwable> serializationIssues;
    private final SerializationCache serializationCache;

    @SuppressWarnings("ThrowableResultIgnored")
    private GradleInfoQueryMap(
            CustomSerializedMap builderMap,
            Map<KeyWrapper, ModelClassPathDef> classpath,
            Map<Object, Throwable> serializationIssues) {
        this.builderMap = builderMap;
        this.classpath = classpath;
        this.serializationCache = SerializationCaches.getDefault();

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
            @Override
            public Object getBuilder(GradleBuildInfoQuery<?> infoQuery) {
                return infoQuery.getInfoBuilder();
            }
        });
    }

    public static GradleInfoQueryMap fromProjectInfos(Map<Object, List<GradleProjectInfoQuery2<?>>> map) {
        return fromQueries(map, new BuilderRetriever<GradleProjectInfoQuery2<?>>() {
            @Override
            public Object getBuilder(GradleProjectInfoQuery2<?> infoQuery) {
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
            List<?> values = entry.getValue().getUnserialized(serializationCache, getClassLoaderForKey(key));
            MultiMapUtils.addAllToMultiMap(key.wrappedKey, values, result);
        }

        for (Map.Entry<Object, Throwable> entry: map.getSerializationProblems().entrySet()) {
            KeyWrapper key = (KeyWrapper)entry.getKey();
            Object issue = issueTransformer.transformIssue(entry.getValue());
            MultiMapUtils.addToMultiMap(key.wrappedKey, issue, result);
        }

        for (Map.Entry<KeyWrapper, Throwable> entry: serializationIssues.entrySet()) {
            Object key = entry.getKey().wrappedKey;
            Object issue = issueTransformer.transformIssue(entry.getValue());
            MultiMapUtils.addToMultiMap(key, issue, result);
        }

        return unsafeCast(result);
    }

    public static IssueTransformer builderIssueTransformer() {
        return BuilderIssueTransformer.INSTANCE;
    }

    public static IssueTransformer buildInfoBuilderIssueTransformer() {
        return BuildInfoBuilderIssueTransformer.INSTANCE;
    }

    private static enum BuildInfoBuilderIssueTransformer implements IssueTransformer {
        INSTANCE;

        @Override
        public Object transformIssue(Throwable issue) {
            return new FailingBuildInfoBuilder(issue);
        }
    }

    private static final class FailingBuildInfoBuilder implements BuildInfoBuilder<Void> {
        private static final long serialVersionUID = 1L;

        private final RuntimeException issue;

        public FailingBuildInfoBuilder(Throwable issue) {
            this.issue = TransferableExceptionWrapper.wrap(issue);
        }

        @Override
        public Void getInfo(BuildController controller) {
            throw issue;
        }

        @Override
        public String getName() {
            return "";
        }
    }

    private static enum BuilderIssueTransformer implements IssueTransformer {
        INSTANCE;

        @Override
        public Object transformIssue(Throwable issue) {
            return new BuilderResult(null, new BuilderIssue("", issue));
        }
    }

    private static final class Deserializer
    implements
            CustomSerializedMap.Deserializer, Serializable {

        private static final long serialVersionUID = 1L;

        private final CustomSerializedMap builderMap;
        private final Map<KeyWrapper, Collection<URL>> paths;

        public Deserializer(
                CustomSerializedMap builderMap,
                Map<KeyWrapper, ModelClassPathDef> classpath) {
            if (builderMap == null) throw new NullPointerException("builderMap");

            this.builderMap = builderMap;
            this.paths = CollectionUtils.newHashMap(classpath.size());
            for (Map.Entry<KeyWrapper, ModelClassPathDef> entry: classpath.entrySet()) {
                KeyWrapper key = entry.getKey();
                if (key == null) throw new NullPointerException("classpath[?].key");

                this.paths.put(key, new ArrayList<URL>(entry.getValue().getJarUrls()));
            }
        }

        private ClassLoader getClassLoaderForKey(
                KeyWrapper key,
                ClassLoader parent,
                Map<Set<String>, ClassLoader> cache) {

            Collection<URL> files = paths.get(key);
            if (files == null || files.isEmpty()) {
                return parent;
            }

            Set<String> urlStrings = new HashSet<String>(2 * files.size());
            for (URL url: files) {
                urlStrings.add(url.toExternalForm());
            }

            ClassLoader result = cache.get(urlStrings);
            if (result != null) {
                return result;
            }

            result = ClassLoaderUtils.classLoaderFromClassPathUrls(files, parent);
            cache.put(urlStrings, result);
            return result;
        }

        @Override
        public Map<Object, List<?>> deserialize(
                SerializationCache serializationCache,
                ClassLoader parent,
                IssueTransformer deserializationIssueTransformer) {
            Map<Set<String>, ClassLoader> cache = new HashMap<Set<String>, ClassLoader>();
            Map<Object, List<?>> result = CollectionUtils.newHashMap(builderMap.size());

            for (Map.Entry<Object, SerializedEntries> entry: builderMap.getMap().entrySet()) {
                KeyWrapper key = (KeyWrapper)entry.getKey();

                ClassLoader classLoader = getClassLoaderForKey(key, parent, cache);
                List<?> deserializedValues;
                try {
                    deserializedValues = entry.getValue().getUnserialized(serializationCache, classLoader);
                } catch (Throwable deserializeEx) {
                    Object issue = deserializationIssueTransformer.transformIssue(deserializeEx);
                    deserializedValues = Collections.singletonList(issue);
                }
                result.put(key, deserializedValues);
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
