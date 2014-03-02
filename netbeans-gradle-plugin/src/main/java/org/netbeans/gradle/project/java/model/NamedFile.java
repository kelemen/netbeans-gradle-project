package org.netbeans.gradle.project.java.model;

import java.io.File;
import org.jtrim.utils.ExceptionHelper;

public final class NamedFile {
    private final File path;
    private final String name;

    public NamedFile(File path, String name) {
        ExceptionHelper.checkNotNullArgument(path, "path");
        ExceptionHelper.checkNotNullArgument(name, "name");

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
