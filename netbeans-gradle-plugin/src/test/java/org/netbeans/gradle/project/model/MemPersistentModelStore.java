
package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MemPersistentModelStore<T> implements PersistentModelStore<T> {
    private final Map<Path, T> models;

    public MemPersistentModelStore() {
        this.models = new ConcurrentHashMap<>();
    }

    public Map<Path, T> getSavedModels() {
        return new HashMap<>(models);
    }

    @Override
    public void persistModel(T model, Path dest) throws IOException {
        models.put(dest, model);
    }

    @Override
    public T tryLoadModel(Path src) throws IOException {
        return models.get(src);
    }
}
