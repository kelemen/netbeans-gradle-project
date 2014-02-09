package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.Serializable;

public final class NbListedDir implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final File directory;

    public NbListedDir(String name, File directory) {
        if (name == null) throw new NullPointerException("name");
        if (directory == null) throw new NullPointerException("directory");

        this.name = name;
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public File getDirectory() {
        return directory;
    }
}
