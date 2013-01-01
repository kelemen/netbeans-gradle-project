package org.netbeans.gradle.project.properties;

import java.io.File;

public interface GradleLocation {
    public static interface Applier {
        public void applyVersion(String versionStr);
        public void applyDirectory(File gradleHome);
        public void applyDefault();
    }

    public void applyLocation(Applier applier);
    public String getUniqueTypeName();
    public String asString();
}
