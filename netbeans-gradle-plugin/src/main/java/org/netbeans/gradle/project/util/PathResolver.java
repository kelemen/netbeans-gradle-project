package org.netbeans.gradle.project.util;

import java.nio.file.Path;

public interface PathResolver {
    public Path resolvePath(Path relativePath);
}
