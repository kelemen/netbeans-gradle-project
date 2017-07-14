package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim2.utils.LazyValues;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleProjectTree;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.NbFileUtils;

public final class NbGradleProjectTree implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GenericProjectProperties genericProperties;
    private final Collection<GradleTaskID> tasks;
    private final Collection<NbGradleProjectTree> children;

    private final AtomicReference<NbGradleProjectTree> parentRef;

    private final Supplier<Map<String, NbGradleProjectTree>> childrenMap;
    private final AtomicInteger numberOfSubprojectsRef;

    public NbGradleProjectTree(
            GenericProjectProperties genericProperties,
            Collection<GradleTaskID> tasks,
            Collection<NbGradleProjectTree> children) {
        this.genericProperties = Objects.requireNonNull(genericProperties, "genericProperties");
        this.tasks = CollectionUtils.copyNullSafeList(tasks);
        this.children = CollectionUtils.copyNullSafeList(children);

        this.childrenMap = LazyValues.lazyValue(this::createChildrenMap);
        this.parentRef = new AtomicReference<>(null);
        this.numberOfSubprojectsRef = new AtomicInteger(-1);
    }

    public NbGradleProjectTree(GradleProjectTree tree) {
        Objects.requireNonNull(tree, "tree");

        this.genericProperties = tree.getGenericProperties();
        this.tasks = tree.getTasks();
        this.children = fromModels(tree.getChildren());

        this.childrenMap = LazyValues.lazyValue(this::createChildrenMap);
        this.parentRef = new AtomicReference<>(null);
        this.numberOfSubprojectsRef = new AtomicInteger(-1);
    }

    public int getNumberOfSubprojects() {
        int result = numberOfSubprojectsRef.get();
        if (result < 0) {
            result = calculateNumberOfSubprojects();
            numberOfSubprojectsRef.set(result);
        }
        return result;
    }

    private int calculateNumberOfSubprojects() {
        int result = 0;
        for (NbGradleProjectTree child: children) {
            result++;
            result += child.getNumberOfSubprojects();
        }
        return result;
    }

    public NbGradleProjectTree getParent(NbGradleProjectTree root) {
        if (root == this) {
            return null;
        }

        NbGradleProjectTree result = parentRef.get();
        if (result == null) {
            root.updateParentRefOfChildren();
            result = parentRef.get();
        }
        return result;
    }

    private void updateParentRefOfChildren() {
        for (NbGradleProjectTree child: children) {
            child.parentRef.set(this);
            child.updateParentRefOfChildren();
        }
    }

    private static Collection<NbGradleProjectTree> fromModels(Collection<GradleProjectTree> models) {
        List<NbGradleProjectTree> result = new ArrayList<>(models.size());
        for (GradleProjectTree model: models) {
            result.add(new NbGradleProjectTree(model));
        }
        return Collections.unmodifiableList(result);
    }

    public static NbGradleProjectTree createEmpty(Path projectDir, ScriptFileProvider scriptProvider) {
        String baseName = NbFileUtils.getFileNameStr(projectDir);
        return new NbGradleProjectTree(
                NbGenericModelInfo.createProjectProperties(baseName, ":" + baseName, projectDir, scriptProvider),
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
        Map<String, NbGradleProjectTree> result = CollectionUtils.newHashMap(children.size());
        for (NbGradleProjectTree child: children) {
            result.put(child.getProjectName(), child);
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, NbGradleProjectTree> getChildrenMap() {
        return childrenMap.get();
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

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final GenericProjectProperties genericProperties;
        private final Collection<GradleTaskID> tasks;
        private final Collection<NbGradleProjectTree> children;

        public SerializedFormat(NbGradleProjectTree source) {
            this.genericProperties = source.genericProperties;
            this.tasks = source.tasks;
            this.children = source.children;
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbGradleProjectTree(genericProperties, tasks, children);
        }
    }
}
