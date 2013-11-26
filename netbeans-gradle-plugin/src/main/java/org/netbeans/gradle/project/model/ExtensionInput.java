package org.netbeans.gradle.project.model;

import java.util.List;
import javax.annotation.CheckForNull;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class ExtensionInput {
    public static final ExtensionInput EMPTY = new ExtensionInput(null, null);

    private final Lookup models;
    private final Object cachedModel;

    public ExtensionInput(List<Object> models, Object cachedModel) {
        this.models = models != null ? Lookups.fixed(models.toArray()) : null;
        this.cachedModel = cachedModel;
    }

    public static ExtensionInput fromModels(List<Object> models) {
        return new ExtensionInput(models, null);
    }

    public static ExtensionInput fromCachedModels(Object cachedModel) {
        return new ExtensionInput(null, cachedModel);
    }

    @CheckForNull
    public Lookup getModels() {
        return models;
    }

    @CheckForNull
    public Object getCachedModel() {
        return cachedModel;
    }
}
