package org.netbeans.gradle.project.api.entry;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class JavaDisablerExtension implements GradleProjectExtension {
    private final boolean disableJava;

    public JavaDisablerExtension(boolean disableJava) {
        this.disableJava = disableJava;
    }

    @Override
    public String getExtensionName() {
        return getClass().getName();
    }

    @Override
    public Iterable<List<Class<?>>> getGradleModels() {
        return Collections.emptyList();
    }

    @Override
    public Lookup getExtensionLookup() {
        return Lookups.fixed(this);
    }

    @Override
    public Set<String> modelsLoaded(Lookup modelLookup) {
        return disableJava
                ? Collections.singleton("org.netbeans.gradle.project.java.JavaExtension")
                : Collections.<String>emptySet();
    }

    @Override
    public Map<File, Lookup> deduceModelsForProjects(Lookup modelLookup) {
        return Collections.emptyMap();
    }
}
