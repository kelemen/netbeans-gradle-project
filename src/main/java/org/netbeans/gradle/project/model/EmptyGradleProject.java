package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.gradle.tooling.internal.gradle.DefaultGradleScript;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleScript;
import org.gradle.tooling.model.GradleTask;
import org.netbeans.gradle.project.GradleProjectConstants;

public final class EmptyGradleProject implements GradleProject {
    private static final DomainObjectSet<GradleTask> NO_TASKS = new EmptySet<GradleTask>();
    private static final DomainObjectSet<GradleProject> NO_PROJECTS = new EmptySet<GradleProject>();

    private final DefaultGradleScript script;
    private final String path;
    private final String name;

    public EmptyGradleProject(File projectDir) {
        this.name = projectDir.getName();
        this.path = ":" + name;
        this.script = new DefaultGradleScript();
        this.script.setSourceFile(new File(projectDir, GradleProjectConstants.BUILD_FILE_NAME));
    }

    @Override
    public DomainObjectSet<? extends GradleTask> getTasks() {
        return NO_TASKS;
    }

    @Override
    public GradleProject getParent() {
        return null;
    }

    @Override
    public DomainObjectSet<? extends GradleProject> getChildren() {
        return NO_PROJECTS;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public GradleProject findByPath(String string) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public GradleScript getBuildScript() {
        return script;
    }

    private static final class EmptySet<E>
    extends
            AbstractSet<E>
    implements
            DomainObjectSet<E> {

        @Override
        public List<E> getAll() {
            return Collections.emptyList();
        }

        @Override
        public E getAt(int i) throws IndexOutOfBoundsException {
            throw new IndexOutOfBoundsException("Index: " + i);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.<E>emptyList().iterator();
        }

        @Override
        public Object[] toArray() {
            return Collections.emptyList().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return Collections.emptyList().toArray(a);
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException("Read-only set.");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Read-only set.");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException("Read-only set.");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only set.");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only set.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Read-only set.");
        }
    }
}
