package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;

public interface ModelPersister<T> {
    public void persistModel(T model, Path dest) throws IOException;
}
