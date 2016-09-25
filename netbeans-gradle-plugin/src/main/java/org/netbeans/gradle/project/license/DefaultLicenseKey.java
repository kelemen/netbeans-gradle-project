package org.netbeans.gradle.project.license;

import java.nio.file.Path;
import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class DefaultLicenseKey {
    private final String name;
    private final Path srcPath;

    public DefaultLicenseKey(String name, Path srcPath) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        ExceptionHelper.checkNotNullArgument(srcPath, "srcPath");

        this.name = name;
        this.srcPath = srcPath;
    }

    public String getName() {
        return name;
    }

    public Path getSrcPath() {
        return srcPath;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(name);
        hash = 23 * hash + Objects.hashCode(srcPath);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final DefaultLicenseKey other = (DefaultLicenseKey)obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.srcPath, other.srcPath);
    }

    @Override
    public String toString() {
        return "License " + name + " at " + srcPath;
    }
}
