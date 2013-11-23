package org.netbeans.gradle.model;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.gradle.model.api.GradleInfoQuery;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;
import org.netbeans.gradle.model.api.ModelClassPathDef;
import org.netbeans.gradle.model.internal.CustomSerializedMap;
import org.netbeans.gradle.model.internal.SerializedEntries;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;

final class GradleInfoQueryMap {
    private final CustomSerializedMap builderMap;
    private final Map<KeyWrapper, ModelClassPathDef> classpath;

    private GradleInfoQueryMap(CustomSerializedMap builderMap, Map<KeyWrapper, ModelClassPathDef> classpath) {
        this.builderMap = builderMap;
        this.classpath = classpath;
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

        return new GradleInfoQueryMap(builders.create(), classpath);
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

    public Map<Object, List<?>> deserializeResults(CustomSerializedMap map) {
        Map<Object, List<?>> result = CollectionUtils.newHashMap(map.size());

        for (Map.Entry<Object, SerializedEntries> entry: map.getMap().entrySet()) {
            KeyWrapper key = (KeyWrapper)entry.getKey();
            List<?> values = entry.getValue().getUnserialized(getClassLoaderForKey(key));
            result.put(key.wrappedKey, values);
        }

        return result;
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
