package org.netbeans.gradle.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class GradleProjectTree {
    private final GenericProjectProperties genericProperties;
    private final Collection<GradleTaskID> tasks;
    private final Collection<GradleProjectTree> children;

    private final AtomicReference<Map<String, GradleProjectTree>> childrenMap;

    public GradleProjectTree(
            GenericProjectProperties genericProperties,
            Collection<GradleTaskID> tasks,
            Collection<GradleProjectTree> children) {
        if (genericProperties == null) throw new NullPointerException("genericProperties");
        this.genericProperties = genericProperties;
        this.tasks = CollectionUtils.copyNullSafeList(tasks);
        this.children = CollectionUtils.copyNullSafeList(children);

        this.childrenMap = new AtomicReference<Map<String, GradleProjectTree>>(null);
    }

    public static GradleProjectTree createEmpty(File projectDir) {
        return new GradleProjectTree(
                new GenericProjectProperties(projectDir.getName(), ":" + projectDir.getName(), projectDir),
                Collections.<GradleTaskID>emptyList(),
                Collections.<GradleProjectTree>emptyList());
    }

    public GenericProjectProperties getGenericProperties() {
        return genericProperties;
    }

    public Collection<GradleTaskID> getTasks() {
        return tasks;
    }

    public String getProjectName() {
        return genericProperties.getProjectName();
    }

    public String getProjectFullName() {
        return genericProperties.getProjectFullName();
    }

    public File getProjectDir() {
        return genericProperties.getProjectDir();
    }

    public Collection<GradleProjectTree> getChildren() {
        return children;
    }

    private Map<String, GradleProjectTree> createChildrenMap() {
        Map<String, GradleProjectTree> result = new HashMap<String, GradleProjectTree>(2 * children.size());
        for (GradleProjectTree child: children) {
            result.put(child.getProjectName(), child);
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, GradleProjectTree> getChildrenMap() {
        Map<String, GradleProjectTree> result = childrenMap.get();
        if (result == null) {
            childrenMap.set(createChildrenMap());
            result = childrenMap.get();
        }
        return result;
    }

    public GradleProjectTree findByPath(String path) {
        GradleProjectTree result = this;
        for (String name: path.split(":")) {
            if (name.length() != 0) {
                result = result.getChildrenMap().get(name);
                if (result == null) {
                    return null;
                }
            }
        }
        return result;
    }
}
