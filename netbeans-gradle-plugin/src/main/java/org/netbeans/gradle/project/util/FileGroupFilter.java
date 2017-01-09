package org.netbeans.gradle.project.util;

import java.nio.file.Path;

public interface FileGroupFilter {
    public boolean isIncluded(Path rootPath, Path file);
    public boolean isAllowAll();
}
