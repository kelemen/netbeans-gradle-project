package org.netbeans.gradle.project.properties2;

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
}
