package org.netbeans.gradle.project;

import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

public final class LoadedExtension {
    private final String name;
    private final DefWithExtension<?> defWithExtension;

    private final Lookup extensionLookup;
    private final DynamicLookup projectLookup;
    private final AtomicBoolean activeState;

    public <ModelType> LoadedExtension(
            GradleProjectExtensionDef<ModelType> extensionDef,
            GradleProjectExtension2<ModelType> extension) {

        if (extensionDef == null) throw new NullPointerException("extensionDef");
        if (extension == null) throw new NullPointerException("extension");

        this.name = extensionDef.getName();
        this.defWithExtension = new DefWithExtension<ModelType>(extensionDef, extension);

        this.projectLookup = new DynamicLookup(extension.getPermanentProjectLookup());
        this.extensionLookup = new ProxyLookup(extension.getExtensionLookup(), this.projectLookup);
        this.activeState = new AtomicBoolean(false);
    }

    public GradleProjectExtensionDef<?> getExtensionDef() {
        return defWithExtension.extensionDef;
    }

    public GradleProjectExtension2<?> getExtension() {
        return defWithExtension.extension;
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

    public void setModelForExtension(Object model) {
        boolean active = model != null;

        if (activeState.compareAndSet(!active, active)) {
            GradleProjectExtension2<?> extension = getExtension();

            if (active) {
                projectLookup.replaceLookups(
                        extension.getPermanentProjectLookup(),
                        extension.getProjectLookup());
                defWithExtension.activate(model);
            }
            else {
                projectLookup.replaceLookups(extension.getPermanentProjectLookup());
                defWithExtension.deactivate();
            }
        }
    }

    private static final class DefWithExtension<ModelType> {
        public final GradleProjectExtensionDef<ModelType> extensionDef;
        public final GradleProjectExtension2<ModelType> extension;
        private final Class<ModelType> modelType;

        public DefWithExtension(
                GradleProjectExtensionDef<ModelType> extensionDef,
                GradleProjectExtension2<ModelType> extension) {

            if (extensionDef == null) throw new NullPointerException("extensionDef");
            if (extension == null) throw new NullPointerException("extension");

            this.extensionDef = extensionDef;
            this.extension = extension;
            this.modelType = extensionDef.getModelType();

            if (this.modelType == null) {
                throw new NullPointerException(
                        "GradleProjectExtensionDef[" + extensionDef.getName() + "].getModelType");
            }
        }

        public void activate(Object model) {
            if (model == null) throw new NullPointerException("model");

            extension.activateExtension(modelType.cast(model));
        }

        public void deactivate() {
            extension.deactivateExtension();
        }
    }
}
