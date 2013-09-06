package org.netbeans.gradle.project.java.model;

import java.util.Locale;
import org.netbeans.gradle.model.java.JavaSourceGroupName;

public final class JavaSourceGroupID {
    private final String sourceSetName;
    private final JavaSourceGroupName groupName;

    public JavaSourceGroupID(String sourceSetName, JavaSourceGroupName groupName) {
        if (sourceSetName == null) throw new NullPointerException("sourceSetName");
        if (groupName == null) throw new NullPointerException("groupName");

        this.sourceSetName = sourceSetName;
        this.groupName = groupName;
    }

    public String getSourceSetName() {
        return sourceSetName;
    }

    public JavaSourceGroupName getGroupName() {
        return groupName;
    }

    public boolean isTest() {
        return sourceSetName.toLowerCase(Locale.US).contains("test");
    }
}
