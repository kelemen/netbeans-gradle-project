package org.netbeans.gradle.project.api.entry;

import java.util.concurrent.atomic.AtomicReference;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public class GradleTestExtension implements GradleProjectExtension2<Object> {
    private final AtomicReference<Lookup> lookupRef;

    public GradleTestExtension() {
        this.lookupRef = new AtomicReference<>(null);
    }

    @Override
    public Lookup getPermanentProjectLookup() {
        Lookup lookup = lookupRef.get();
        if (lookup == null) {
            lookupRef.compareAndSet(null, Lookups.fixed(this));
            lookup = lookupRef.get();
        }
        return lookup;
    }

    @Override
    public Lookup getProjectLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public void activateExtension(Object parsedModel) {
    }

    @Override
    public void deactivateExtension() {
    }

    @Override
    public Lookup getExtensionLookup() {
        return Lookup.EMPTY;
    }
}
