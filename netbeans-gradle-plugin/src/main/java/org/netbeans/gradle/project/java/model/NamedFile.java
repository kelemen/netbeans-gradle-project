package org.netbeans.gradle.project.java.model;

import java.io.File;

public final class NamedFile {
    private final File path;
    private final String name;

    public NamedFile(File path, String name) {
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
