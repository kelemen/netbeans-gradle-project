package org.netbeans.gradle.project.java.model;

import java.io.File;

public final class NbSourceRoot {
    private final File path;
    private final String name;

    public NbSourceRoot(File path, String name) {
        if (path == null) throw new NullPointerException("path");
        if (name == null) throw new NullPointerException("name");
        this.path = path;
        this.name = name;
    }

    public File getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
}
