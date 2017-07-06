package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.utils.ExceptionHelper;

public final class NbJarOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_JAR_TASK_NAME = "jar";

    private final String taskName;
    private final File jar;
    private final Set<File> classDirs;

    public NbJarOutput(String taskName, File jar, Collection<? extends File> classDirs) {
        this.taskName = Objects.requireNonNull(taskName, "taskName");
        this.jar = Objects.requireNonNull(jar, "jar");
        this.classDirs = Collections.unmodifiableSet(new HashSet<>(classDirs));

        ExceptionHelper.checkNotNullElements(this.classDirs, "classDirs");
    }

    public String getTaskName() {
        return taskName;
    }

    public File getJar() {
        return jar;
    }

    public Set<File> getClassDirs() {
        return classDirs;
    }

    public boolean isDefaultJar() {
        return DEFAULT_JAR_TASK_NAME.equalsIgnoreCase(taskName);
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String taskName;
        private final File jar;
        private final Set<File> classDirs;

        public SerializedFormat(NbJarOutput source) {
            this.taskName = source.taskName;
            this.jar = source.jar;
            this.classDirs = source.classDirs;
        }


        private Object readResolve() throws ObjectStreamException {
            return new NbJarOutput(taskName, jar, classDirs);
        }
    }
}
