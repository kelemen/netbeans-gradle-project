package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleProjectTree;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class NbGradleProjectTree {
    private final GenericProjectProperties genericProperties;
    private final Collection<GradleTaskID> tasks;
    private final Collection<NbGradleProjectTree> children;

    private final AtomicReference<Map<String, NbGradleProjectTree>> childrenMap;

    public NbGradleProjectTree(
            GenericProjectProperties genericProperties,
            Collection<GradleTaskID> tasks,
            Collection<NbGradleProjectTree> children) {
        if (genericProperties == null) throw new NullPointerException("genericProperties");
        this.genericProperties = genericProperties;
        this.tasks = CollectionUtils.copyNullSafeList(tasks);
        this.children = CollectionUtils.copyNullSafeList(children);

        this.childrenMap = new AtomicReference<Map<String, NbGradleProjectTree>>(null);
    }

    public NbGradleProjectTree(GradleProjectTree tree) {
        if (tree == null) throw new NullPointerException("tree");

        this.genericProperties = tree.getGenericProperties();
        this.tasks = tree.getTasks();
        this.children = fromModels(tree.getChildren());

        this.childrenMap = new AtomicReference<Map<String, NbGradleProjectTree>>(null);
    }

    private static Collection<NbGradleProjectTree> fromModels(Collection<GradleProjectTree> models) {
        List<NbGradleProjectTree> result = new ArrayList<NbGradleProjectTree>(models.size());
        for (GradleProjectTree model: models) {
            result.add(new NbGradleProjectTree(model));
        }
        return Collections.unmodifiableList(result);
    }

    public static NbGradleProjectTree createEmpty(File projectDir) {
        return new NbGradleProjectTree(
                new GenericProjectProperties(projectDir.getName(), ":" + projectDir.getName(), projectDir),
                Collections.<GradleTaskID>emptyList(),
                Collections.<NbGradleProjectTree>emptyList());
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

    public Collection<NbGradleProjectTree> getChildren() {
        return children;
    }

    private Map<String, NbGradleProjectTree> createChildrenMap() {
        Map<String, NbGradleProjectTree> result = new HashMap<String, NbGradleProjectTree>(2 * children.size());
        for (NbGradleProjectTree child: children) {
            result.put(child.getProjectName(), child);
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, NbGradleProjectTree> getChildrenMap() {
        Map<String, NbGradleProjectTree> result = childrenMap.get();
        if (result == null) {
            childrenMap.set(createChildrenMap());
            result = childrenMap.get();
        }
        return result;
    }

    public NbGradleProjectTree findByPath(String path) {
        NbGradleProjectTree result = this;
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
