package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.NbGradleProject;

public enum GradleLocationDefault implements GradleLocation {
    INSTANCE;

    public static final String UNIQUE_TYPE_NAME = "DEFAULT";

    @Override
    public void applyLocation(NbGradleProject project, Applier applier) {
        applier.applyDefault();
    }

    @Override
    public String asString() {
        return null;
    }

    @Override
    public String getUniqueTypeName() {
        return UNIQUE_TYPE_NAME;
    }
}
