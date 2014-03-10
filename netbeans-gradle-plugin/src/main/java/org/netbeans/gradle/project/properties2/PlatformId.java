package org.netbeans.gradle.project.properties2;

import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class PlatformId {
    public static final String DEFAULT_NAME = "j2se";

    private final String name;
    private final String version;

    public PlatformId(String name, String version) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        ExceptionHelper.checkNotNullArgument(version, "version");

        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.name);
        hash = 29 * hash + Objects.hashCode(this.version);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final PlatformId other = (PlatformId)obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
        return "PlatformId{" + "name=" + name + ", version=" + version + '}';
    }
}
