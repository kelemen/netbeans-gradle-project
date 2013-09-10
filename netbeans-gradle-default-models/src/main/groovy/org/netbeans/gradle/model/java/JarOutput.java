package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;

public final class JarOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String taskName;
    private final File jar;

    public JarOutput(String taskName, File jar) {
        if (taskName == null) throw new NullPointerException("taskName");
        if (jar == null) throw new NullPointerException("jar");

        this.taskName = taskName;
        this.jar = jar;
    }

    public String getTaskName() {
        return taskName;
    }

    public File getJar() {
        return jar;
    }
}
