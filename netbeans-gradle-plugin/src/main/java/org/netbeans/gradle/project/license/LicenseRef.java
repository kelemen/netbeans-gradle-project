package org.netbeans.gradle.project.license;

import java.util.Objects;

public final class LicenseRef {
    private final String id;
    private final String displayName;
    private final boolean dynamic;

    public LicenseRef(String id, String displayName, boolean dynamic) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
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
