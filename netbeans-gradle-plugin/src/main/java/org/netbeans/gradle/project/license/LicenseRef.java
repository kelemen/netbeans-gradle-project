package org.netbeans.gradle.project.license;

import org.jtrim.utils.ExceptionHelper;

public final class LicenseRef {
    private final String id;
    private final String displayName;
    private final boolean dynamic;

    public LicenseRef(String id, String displayName, boolean dynamic) {
        ExceptionHelper.checkNotNullArgument(id, "id");
        ExceptionHelper.checkNotNullArgument(displayName, "displayName");

        this.id = id;
        this.displayName = displayName;
        this.dynamic = dynamic;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public String toString() {
        return "License " + id + (dynamic ? " (dynamically created)" : "");
    }
}
