package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public final class NbListedDir implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final File directory;

    public NbListedDir(String name, File directory) {
        this.name = Objects.requireNonNull(name, "name");
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public String getName() {
        return name;
    }

    public File getDirectory() {
        return directory;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + name.hashCode();
        hash = 23 * hash + directory.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final NbListedDir other = (NbListedDir)obj;
        if (!name.equals(other.name)) {
            return false;
        }
        return directory == other.directory || directory.equals(other.directory);
    }
}
