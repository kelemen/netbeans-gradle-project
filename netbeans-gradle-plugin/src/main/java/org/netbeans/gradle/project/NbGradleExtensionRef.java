package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.entry.ParsedModel;
import org.netbeans.gradle.project.model.RequestedProjectDir;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class NbGradleExtensionRef {
    private static final Logger LOGGER = Logger.getLogger(NbGradleExtensionRef.class.getName());

    private final String name;
    private final DefWithExtension<?> defWithExtension;

    private final DynamicLookup extensionLookup;
    private final DynamicLookup projectLookup;
    private final AtomicBoolean activeState;

    public <ModelType> NbGradleExtensionRef(
            GradleProjectExtensionDef<ModelType> extensionDef,
            GradleProjectExtension2<ModelType> extension) {

        if (extensionDef == null) throw new NullPointerException("extensionDef");
        if (extension == null) throw new NullPointerException("extension");

        this.name = extensionDef.getName();
        this.defWithExtension = new DefWithExtension<ModelType>(extensionDef, extension);

        this.projectLookup = new DynamicLookup(extension.getPermanentProjectLookup());
        this.extensionLookup = new DynamicLookup();
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

    private ParsedModel<?> safelyReturn(ParsedModel<?> result) {
        if (result == null) {
            LOGGER.log(Level.WARNING,
                    "GradleProjectExtensionDef.parseModel returned null for extension {0}",
                    getName());
            return ParsedModel.noModels();
        }
        else {
            return result;
        }
    }

    public ParsedModel<?> parseModel(
            RequestedProjectDir mainProjectDir,
            Collection<?> retrievedModels) {
        if (mainProjectDir == null) throw new NullPointerException("mainProjectDir");

        Object[] models = retrievedModels.toArray(new Object[retrievedModels.size() + 1]);
        models[models.length - 1] = mainProjectDir;
        return parseModel(Lookups.fixed(models));
    }

    private ParsedModel<?> parseModel(Lookup retrievedModels) {
        return safelyReturn(getExtensionDef().parseModel(retrievedModels));
    }

    public void setModelForExtension(Object model) {
        boolean active = model != null;

        if (activeState.compareAndSet(!active, active)) {
            GradleProjectExtension2<?> extension = getExtension();

            if (active) {
                projectLookup.replaceLookups(
                        extension.getPermanentProjectLookup(),
                        extension.getProjectLookup());
                extensionLookup.replaceLookups(
                        extension.getExtensionLookup(),
                        extension.getPermanentProjectLookup(),
                        extension.getProjectLookup());

                defWithExtension.activate(model);
            }
            else {
                projectLookup.replaceLookups(extension.getPermanentProjectLookup());
                extensionLookup.replaceLookups(Lookup.EMPTY);
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
