package org.netbeans.gradle.project.api.entry;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author radim
 */
public class GradleTestExtension implements GradleProjectExtension {
    private final AtomicReference<Lookup> lookupRef;

    public GradleTestExtension() {
        this.lookupRef = new AtomicReference<Lookup>(null);
    }

    @Override
    public Iterable<List<Class<?>>> getGradleModels() {
        return Collections.<List<Class<?>>>singleton(Collections.<Class<?>>emptyList());
    }

    @Override
    public Lookup getExtensionLookup() {
        Lookup lookup = lookupRef.get();
        if (lookup == null) {
            lookupRef.compareAndSet(null, Lookups.fixed(this));
            lookup = lookupRef.get();
        }
        return lookup;
    }

    @Override
    public Set<String> modelsLoaded(Lookup modelLookup) {
        return null;
    }

    @Override
    public String getExtensionName() {
        return getClass().getName();
    }

    @Override
    public Map<File, Lookup> deduceModelsForProjects(Lookup modelLookup) {
        return Collections.emptyMap();
    }
}
