package org.netbeans.gradle.project.groovy;

import java.util.function.Supplier;
import org.jtrim2.utils.LazyValues;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.modules.groovy.support.spi.GroovyExtenderImplementation;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GroovyExtension implements GradleProjectExtension2<NbGroovyModel> {
    private final Supplier<Lookup> projectLookupRef;

    public GroovyExtension(Project project) {
        this.projectLookupRef = LazyValues.lazyValue(() -> {
            return Lookups.fixed(DefaultGroovyExtenderImplementation.INSTANCE);
        });
    }

    @Override
    public Lookup getPermanentProjectLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public Lookup getProjectLookup() {
        return projectLookupRef.get();
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
