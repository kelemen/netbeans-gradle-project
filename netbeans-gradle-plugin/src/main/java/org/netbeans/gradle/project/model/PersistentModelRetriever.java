package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;

public interface PersistentModelRetriever<T> {
    public T tryLoadModel(Path src) throws IOException;
}
