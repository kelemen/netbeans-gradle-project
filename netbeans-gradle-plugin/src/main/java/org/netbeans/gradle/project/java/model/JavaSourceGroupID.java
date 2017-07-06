package org.netbeans.gradle.project.java.model;

import java.util.Locale;
import java.util.Objects;
import org.netbeans.gradle.model.java.JavaSourceGroupName;

public final class JavaSourceGroupID {
    private final String sourceSetName;
    private final JavaSourceGroupName groupName;

    public JavaSourceGroupID(String sourceSetName, JavaSourceGroupName groupName) {
        this.sourceSetName = Objects.requireNonNull(sourceSetName, "sourceSetName");
        this.groupName = Objects.requireNonNull(groupName, "groupName");
    }

    public String getSourceSetName() {
        return sourceSetName;
    }

    public JavaSourceGroupName getGroupName() {
        return groupName;
    }

    public boolean isTest() {
        return isTestSourceSet(sourceSetName);
    }

    public static boolean isTestSourceSet(String sourceSetName) {
        return sourceSetName.toLowerCase(Locale.US).contains("test");
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + sourceSetName.hashCode();
        hash = 53 * hash + groupName.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final JavaSourceGroupID other = (JavaSourceGroupID)obj;
        if (!this.sourceSetName.equals(other.sourceSetName)) {
            return false;
        }
        return this.groupName == other.groupName;
    }
}
