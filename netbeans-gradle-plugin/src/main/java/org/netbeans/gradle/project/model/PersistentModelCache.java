package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.util.Collection;

public interface PersistentModelCache<T> {
    public T tryGetModel(PersistentModelKey modelKey) throws IOException;
    public void saveGradleModels(Collection<? extends T> models) throws IOException;
}
