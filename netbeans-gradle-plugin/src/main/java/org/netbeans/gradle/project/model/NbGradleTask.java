package org.netbeans.gradle.project.model;

import java.util.Objects;

public final class NbGradleTask {
    private final String qualifiedName;
    private final String localName;
    private final String description;

    public NbGradleTask(String qualifiedName, String description) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "qualifiedName");
        this.localName = getLocalName(qualifiedName);
        this.description = Objects.requireNonNull(description, "description");
    }

    private static String getLocalName(String qualifiedName) {
        int separator = qualifiedName.lastIndexOf(':');
        if (separator >= 0) {
            return qualifiedName.substring(separator + 1, qualifiedName.length());
        }
        else {
            return qualifiedName;
        }
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getLocalName() {
        return localName;
    }

    public String getDescription() {
        return description;
    }
}
