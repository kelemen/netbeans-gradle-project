package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Objects;

public final class NamedFile {
    private final File path;
    private final String name;

    public NamedFile(File path, String name) {
        this.path = Objects.requireNonNull(path, "path");
        this.name = Objects.requireNonNull(name, "name");
    }

    public File getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
}
