package org.netbeans.gradle.project;

import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

public final class LoadedExtension {
    private final String name;
    private final GradleProjectExtensionDef extensionDef;
    private final GradleProjectExtension2 extension;

    private final Lookup extensionLookup;
    private final DynamicLookup projectLookup;
    private final AtomicBoolean activeState;

    public LoadedExtension(GradleProjectExtensionDef extensionDef, GradleProjectExtension2 extension) {
        if (extensionDef == null) throw new NullPointerException("extensionDef");
        if (extension == null) throw new NullPointerException("extension");

        this.name = extensionDef.getName();
        this.extensionDef = extensionDef;
        this.extension = extension;

        this.projectLookup = new DynamicLookup(extension.getPermanentProjectLookup());
        this.extensionLookup = new ProxyLookup(extension.getExtensionLookup(), this.projectLookup);
        this.activeState = new AtomicBoolean(false);
    }

    public GradleProjectExtensionDef getExtensionDef() {
        return extensionDef;
    }

    public GradleProjectExtension2 getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    public Lookup getExtensionLookup() {
        return extensionLookup;
    }

    public Lookup getProjectLookup() {
        return projectLookup;
    }

    public void setActive(boolean active) {
        if (activeState.compareAndSet(!active, active)) {
            if (active) {
                projectLookup.replaceLookups(
                        extension.getPermanentProjectLookup(),
                        extension.getProjectLookup());
            }
            else {
                projectLookup.replaceLookups(extension.getPermanentProjectLookup());
            }
        }
    }
}
