package org.netbeans.gradle.project.extensions;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.utils.LazyValues;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.entry.ModelLoadResult;
import org.netbeans.gradle.project.api.entry.ParsedModel;
import org.netbeans.gradle.project.lookups.DynamicLookup;
import org.openide.util.Lookup;

public final class NbGradleExtensionRef {
    private static final Logger LOGGER = Logger.getLogger(NbGradleExtensionRef.class.getName());

    private final String name;
    private final String displayName;
    private final DefWithExtension<?> defWithExtension;
    private final ModelNeeds modelNeed;
    private final AtomicBoolean lastActive;
    private final DeducedExtensionServicesProvider deducedServicesProvider;

    private final DynamicLookup extensionLookup;
    private final DynamicLookup projectLookup;
    private final Supplier<Lookup> deducedServicesRef;

    public <ModelType> NbGradleExtensionRef(
            GradleProjectExtensionDef<ModelType> extensionDef,
            GradleProjectExtension2<ModelType> extension,
            DeducedExtensionServicesProvider deducedServicesProvider) {

        Objects.requireNonNull(extensionDef, "extensionDef");
        Objects.requireNonNull(extension, "extension");
        Objects.requireNonNull(deducedServicesProvider, "deducedServicesProvider");

        this.name = extensionDef.getName();
        checkExtensionName(name, extensionDef);

        this.deducedServicesProvider = deducedServicesProvider;
        this.displayName = useNameIfNoDisplayName(extensionDef.getDisplayName(), name);
        this.defWithExtension = new DefWithExtension<>(extensionDef, extension);
        this.modelNeed = new ModelNeeds(extensionDef);

        this.projectLookup = new DynamicLookup(extension.getPermanentProjectLookup());
        this.extensionLookup = new DynamicLookup();
        this.lastActive = new AtomicBoolean(false);
        this.deducedServicesRef = LazyValues.lazyValue(this::createDeducedLookup);
    }

    private static void checkExtensionName(String name, GradleProjectExtensionDef<?> def) {
        if (name == null) {
            throw new NullPointerException("Extension name cannot be null for " + def.getClass().getName());
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Extension name cannot be empty for " + def.getClass().getName());
        }
    }

    private static String useNameIfNoDisplayName(String displayName, String name) {
        if (displayName == null) {
            LOGGER.log(Level.WARNING,
                    "GradleProjectExtensionDef.getDisplayName returned null for extension {0}",
                    name);
            return name;
        }
        return displayName;
    }

    public ModelNeeds getModelNeeds() {
        return modelNeed;
    }

    public GradleProjectExtensionDef<?> getExtensionDef() {
        return defWithExtension.extensionDef;
    }

    public GradleProjectExtension2<?> getExtension() {
        return defWithExtension.extension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return name;
    }

    public Lookup getExtensionLookup() {
        return extensionLookup.getUnmodifiableView();
    }

    public Lookup getProjectLookup() {
        return projectLookup.getUnmodifiableView();
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

    public ParsedModel<?> parseModel(ModelLoadResult retrievedModels) {
        Objects.requireNonNull(retrievedModels, "retrievedModels");

        try {
            return safelyReturn(getExtensionDef().parseModel(retrievedModels));
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Extension failed to parse models: " + getName(), ex);
            return ParsedModel.noModels();
        }
    }

    private Lookup createDeducedLookup() {
        GradleProjectExtension2<?> extension = getExtension();
        return deducedServicesProvider.getDeducedLookup(this,
                extension.getExtensionLookup(),
                extension.getPermanentProjectLookup(),
                extension.getProjectLookup());
    }

    public boolean setModelForExtension(Object model) {
        boolean active = model != null;
        boolean prevActive = lastActive.getAndSet(active);

        GradleProjectExtension2<?> extension = getExtension();

        if (active) {
            projectLookup.replaceLookups(
                    extension.getPermanentProjectLookup(),
                    extension.getProjectLookup());
            extensionLookup.replaceLookups(
                    deducedServicesRef.get(),
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

        return prevActive != active;
    }

    private static final class DefWithExtension<ModelType> {
        public final GradleProjectExtensionDef<ModelType> extensionDef;
        public final GradleProjectExtension2<ModelType> extension;
        private final Class<ModelType> modelType;

        public DefWithExtension(
                GradleProjectExtensionDef<ModelType> extensionDef,
                GradleProjectExtension2<ModelType> extension) {

            this.extensionDef = Objects.requireNonNull(extensionDef, "extensionDef");
            this.extension = Objects.requireNonNull(extension, "extension");
            this.modelType = extensionDef.getModelType();

            if (this.modelType == null) {
                throw new NullPointerException(
                        "GradleProjectExtensionDef[" + extensionDef.getName() + "].getModelType");
            }
        }

        public void activate(Object model) {
            Objects.requireNonNull(model, "model");
            extension.activateExtension(modelType.cast(model));
        }

        public void deactivate() {
            extension.deactivateExtension();
        }
    }
}
