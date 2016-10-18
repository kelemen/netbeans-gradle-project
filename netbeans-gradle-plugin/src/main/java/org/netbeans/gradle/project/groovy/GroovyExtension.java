package org.netbeans.gradle.project.groovy;

import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.modules.groovy.support.spi.GroovyExtenderImplementation;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GroovyExtension implements GradleProjectExtension2<NbGroovyModel> {
    private final AtomicReference<Lookup> projectLookupRef;

    public GroovyExtension(Project project) {
        this.projectLookupRef = new AtomicReference<>(null);
    }

    @Override
    public Lookup getPermanentProjectLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public Lookup getProjectLookup() {
        Lookup lookup = projectLookupRef.get();
        if (lookup == null) {
            lookup = Lookups.fixed(DefaultGroovyExtenderImplementation.INSTANCE);
            if (!projectLookupRef.compareAndSet(null, lookup)) {
                lookup = projectLookupRef.get();
            }
        }
        return lookup;
    }
    @Override
    public Lookup getExtensionLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public void activateExtension(NbGroovyModel parsedModel) {
    }

    @Override
    public void deactivateExtension() {
    }

    private enum DefaultGroovyExtenderImplementation implements GroovyExtenderImplementation {
        INSTANCE;

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }
    }
}
