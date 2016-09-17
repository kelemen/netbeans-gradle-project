package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.config.ExtensionSettingsId;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.entry.ModelLoadResult;
import org.netbeans.gradle.project.api.entry.ParsedModel;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery1;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.properties.ExtensionProjectSettingsPageDefs;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class ExtensionLoader {
    private static final Logger LOGGER = Logger.getLogger(ExtensionLoader.class.getName());
    private static final SpecificationVersion JAVA6_VERSION = new SpecificationVersion("1.6");

    private static Level levelFromException(Throwable exception) {
        if (exception instanceof RuntimeException) {
            return Level.WARNING;
        }
        else if (exception instanceof Error) {
            return Level.SEVERE;
        }
        else {
            return Level.INFO;
        }
    }

    /** @deprecated */
    @Deprecated
    @SuppressWarnings("UseSpecificCatch")
    private static NbGradleExtensionRef loadExtension(
            NbGradleProject project,
            org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery def) {

        org.netbeans.gradle.project.api.entry.GradleProjectExtension extension = null;

        try {
            extension = def.loadExtensionForProject(project);
            Objects.requireNonNull(extension, "def.loadExtensionForProject");

            File projectDir = project.getProjectDirectoryAsFile();
            GradleProjectExtensionDef<SerializableLookup> def2
                    = createWrappedDef(projectDir, def, extension);
            GradleProjectExtension2<SerializableLookup> extension2
                    = createWrappedProjectExtension(extension);
            return createExtensionRef(project, def2, extension2);
        } catch (Throwable ex) {
            String name = extension != null
                    ? extension.getExtensionName()
                    : def.getClass().getName();

            LOGGER.log(levelFromException(ex),
                    "Failed to load extension: " + name + " for project " + project.getProjectDirectory(),
                    ex);
            return null;
        }
    }

    private static <ModelType> NbGradleExtensionRef loadExtension(
            NbGradleProject project,
            GradleProjectExtensionDef<ModelType> def) throws IOException {

        try {
            GradleProjectExtension2<ModelType> extension = def.createExtension(project);
            return createExtensionRef(project, def, extension);
        } catch (Throwable ex) {
            LOGGER.log(levelFromException(ex),
                    "Failed to load extension: " + def.getName() + " for project " + project.getProjectDirectory(),
                    ex);
            return null;
        }
    }

    private static <T> T tryGet(Class<? extends T> type, Lookup... lookups) {
        for (Lookup lookup: lookups) {
            T result = lookup.lookup(type);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static String getExtensionNameForConfig(NbGradleExtensionRef extensionRef, Lookup... lookups) {
        ExtensionSettingsId settingsId = tryGet(ExtensionSettingsId.class, lookups);
        return settingsId != null ? settingsId.getId() : extensionRef.getName();
    }

    private static <ModelType> NbGradleExtensionRef createExtensionRef(
            final NbGradleProject project,
            GradleProjectExtensionDef<ModelType> def,
            GradleProjectExtension2<ModelType> extension) {

        return new NbGradleExtensionRef(def, extension, new DeducedExtensionServicesProvider() {
            @Override
            public Lookup getDeducedLookup(NbGradleExtensionRef extensionRef, Lookup... lookups) {
                String extensionName = getExtensionNameForConfig(extensionRef, lookups);
                return Lookups.fixed(new ExtensionProjectSettingsPageDefs(project, extensionName, lookups));
            }
        });
    }

    private static void tryAddExtension(
            Object sourceDef,
            NbGradleExtensionRef extension,
            List<NbGradleExtensionRef> result,
            Set<String> alreadyLoaded) {
        if (extension == null) {
            return;
        }

        if (alreadyLoaded.contains(extension.getName())) {
            LOGGER.log(Level.WARNING, "Extension has already been loaded with the same name: {0}. Ignoring implementation: {1}",
                    new Object[]{extension.getName(), sourceDef.getClass().getName()});
        }
        else {
            result.add(extension);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<GradleProjectExtensionDef<?>> defClass() {
        return (Class<GradleProjectExtensionDef<?>>)(Class<?>)GradleProjectExtensionDef.class;
    }

    @SuppressWarnings("deprecation")
    public static List<NbGradleExtensionRef> loadExtensions(NbGradleProject project) throws IOException {
        Lookup defaultLookup = Lookup.getDefault();

        Collection<? extends org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery> defs1
                = defaultLookup.lookupAll(org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery.class);

        Collection<? extends GradleProjectExtensionDef<?>> defs2
                = defaultLookup.lookupAll(defClass());

        int expectedExtensionCount = defs1.size() + defs2.size();
        List<NbGradleExtensionRef> result = new ArrayList<>(expectedExtensionCount);

        Set<String> alreadyLoaded = CollectionUtils.newHashSet(expectedExtensionCount);
        for (GradleProjectExtensionDef<?> def: defs2) {
            NbGradleExtensionRef loadedExtension = loadExtension(project, def);
            tryAddExtension(def, loadedExtension, result, alreadyLoaded);
        }

        for (org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery def: defs1) {
            NbGradleExtensionRef loadedExtension = loadExtension(project, def);
            tryAddExtension(def, loadedExtension, result, alreadyLoaded);
        }

        return result;
    }

    /** @deprecated */
    @Deprecated
    private static GradleModelDefQuery1 getModelQuery(
            org.netbeans.gradle.project.api.entry.GradleProjectExtension extension) {

        List<Class<?>> allModels = new LinkedList<>();
        for (List<Class<?>> models: extension.getGradleModels()) {
            allModels.addAll(models);
        }

        final List<Class<?>> toolingModels = Collections.unmodifiableList(allModels);
        return new GradleModelDefQuery1() {
            @Override
            public Collection<Class<?>> getToolingModels(GradleTarget gradleTarget) {
                if (gradleTarget.getJavaVersion().compareTo(JAVA6_VERSION) < 0) {
                    return Collections.emptyList();
                }

                return toolingModels;
            }
        };
    }

    /** @deprecated */
    @Deprecated
    private static GradleProjectExtension2<SerializableLookup> createWrappedProjectExtension(
            final org.netbeans.gradle.project.api.entry.GradleProjectExtension extension) {

        final Lookup permanentLookup = extension.getExtensionLookup();

        return new GradleProjectExtension2<SerializableLookup>() {
            @Override
            public Lookup getPermanentProjectLookup() {
                return permanentLookup;
            }

            @Override
            public Lookup getProjectLookup() {
                return Lookup.EMPTY;
            }

            @Override
            public Lookup getExtensionLookup() {
                return Lookup.EMPTY;
            }

            @Override
            public void activateExtension(SerializableLookup parsedModel) {
                extension.modelsLoaded(parsedModel.lookup);
            }

            @Override
            public void deactivateExtension() {
                extension.modelsLoaded(Lookup.EMPTY);
            }
        };
    }

    private static SerializableLookup serializableLookup(Object... content) {
        return new SerializableLookup(Lookups.fixed(content));
    }

    private static SerializableLookup serializableLookup(Collection<?> content) {
        return serializableLookup(content.toArray());
    }

    private static Map<File, SerializableLookup> serializableLookupMap(Map<File, Lookup> map) {
        Map<File, SerializableLookup> result = CollectionUtils.newHashMap(map.size());
        for (Map.Entry<File, Lookup> entry: map.entrySet()) {
            result.put(entry.getKey(), new SerializableLookup(entry.getValue()));
        }
        return result;
    }

    /** @deprecated */
    @Deprecated
    private static ParsedModel<SerializableLookup> parseModelUsingExtension(
            File projectDir,
            org.netbeans.gradle.project.api.entry.GradleProjectExtension extension,
            Lookup retrievedModels) {

        Map<File, SerializableLookup> deduced = serializableLookupMap(
                extension.deduceModelsForProjects(retrievedModels));

        if (!deduced.containsKey(projectDir)) {
            List<Object> lookupContent = new LinkedList<>();
            for (List<Class<?>> models: extension.getGradleModels()) {
                for (Class<?> neededModel: models) {
                    Object model = retrievedModels.lookup(neededModel);
                    if (model != null) {
                        lookupContent.add(model);
                        break;
                    }
                }
            }

            return new ParsedModel<>(serializableLookup(lookupContent), deduced);
        }

        SerializableLookup mainModels = deduced.get(projectDir);
        if (mainModels != null) {
            deduced = new HashMap<>(deduced);
            deduced.remove(projectDir);
        }

        return new ParsedModel<>(mainModels, deduced);
    }

    /** @deprecated  */
    @Deprecated
    private static GradleProjectExtensionDef<SerializableLookup> createWrappedDef(
            final File projectDirOfExtension,
            final org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery query,
            final org.netbeans.gradle.project.api.entry.GradleProjectExtension extension) {

        return new OldExtensionQueryWrapper(extension, projectDirOfExtension, query);
    }

    private ExtensionLoader() {
        throw new AssertionError();
    }

    /** @deprecated */
    @Deprecated
    @SuppressWarnings("deprecation")
    private static class OldExtensionQueryWrapper implements GradleProjectExtensionDef<SerializableLookup> {
        private final String extensionName;
        private final org.netbeans.gradle.project.api.entry.GradleProjectExtension extension;
        private final Lookup lookup;
        private final File projectDirOfExtension;
        private final org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery query;

        /** @deprecated */
        @Deprecated
        public OldExtensionQueryWrapper(
                org.netbeans.gradle.project.api.entry.GradleProjectExtension extension,
                File projectDirOfExtension,
                org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery query) {

            this.extensionName = extension.getExtensionName();
            this.extension = extension;
            this.lookup = Lookups.singleton(getModelQuery(extension));
            this.projectDirOfExtension = projectDirOfExtension;
            this.query = query;

            Objects.requireNonNull(extensionName, "GradleProjectExtension.getExtensionName()");
        }

        @Override
        public String getName() {
            return extensionName;
        }

        @Override
        public String getDisplayName() {
            return extension.getExtensionName();
        }

        @Override
        public Lookup getLookup() {
            return lookup;
        }

        @Override
        public Class<SerializableLookup> getModelType() {
            return SerializableLookup.class;
        }

        @Override
        public ParsedModel<SerializableLookup> parseModel(ModelLoadResult modelLoadResult) {
            File projectDir = modelLoadResult.getMainProjectDir();
            Lookup models = modelLoadResult.getMainProjectModels();

            if (projectDir.equals(projectDirOfExtension)) {
                return parseModelUsingExtension(projectDir, extension, models);
            }

            NbGradleProject project = NbGradleProjectFactory.tryLoadSafeGradleProject(projectDir);
            if (project == null) {
                LOGGER.log(Level.WARNING, "Could not load Gradle project: {0}", projectDir);
                return ParsedModel.noModels();
            }

            NbGradleExtensionRef extensionToUse = null;
            for (NbGradleExtensionRef projectExtension: project.getExtensionRefs()) {
                if (extensionName.equals(projectExtension.getName())) {
                    extensionToUse = projectExtension;
                    break;
                }
            }

            if (extensionToUse == null) {
                LOGGER.log(Level.WARNING, "Missing extension for project: {0}", projectDir);
                return ParsedModel.noModels();
            }

            GradleProjectExtensionDef<?> extensionDefToUse = extensionToUse.getExtensionDef();
            if (extensionDefToUse instanceof OldExtensionQueryWrapper) {
                OldExtensionQueryWrapper wrapper = (OldExtensionQueryWrapper)extensionDefToUse;

                return parseModelUsingExtension(projectDir, wrapper.extension, models);
            }

            LOGGER.log(Level.WARNING,
                    "Other project does not uses the old format for the same extension. Extension = {0}, Project: {1}",
                    new Object[]{extensionName, projectDir});
            return ParsedModel.noModels();
        }

        @Override
        public GradleProjectExtension2<SerializableLookup> createExtension(Project project) throws IOException {
            // This won't really be called, only implemented for completness sake.
            org.netbeans.gradle.project.api.entry.GradleProjectExtension newExtension
                    = query.loadExtensionForProject(project);

            Objects.requireNonNull(newExtension, "GradleProjectExtensionQuery.loadExtensionForProject");
            return createWrappedProjectExtension(newExtension);
        }

        @Override
        public Set<String> getSuppressedExtensions() {
            return Collections.emptySet();
        }
    }

    private static final class SerializableLookup implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Lookup lookup;

        public SerializableLookup(Lookup lookup) {
            ExceptionHelper.checkNotNullArgument(lookup, "lookup");

            this.lookup = lookup;
        }

        public <T> T lookup(Class<T> clazz) {
            return lookup.lookup(clazz);
        }

        public <T> Collection<? extends T> lookupAll(Class<T> clazz) {
            return lookup.lookupAll(clazz);
        }

        private Object writeReplace() {
            return new SerializedFormat(this);
        }

        private void readObject(ObjectInputStream stream) throws InvalidObjectException {
            throw new InvalidObjectException("Use proxy.");
        }

        private static final class SerializedFormat implements Serializable {
            private static final long serialVersionUID = 1L;

            private final Object[] lookupContent;

            public SerializedFormat(SerializableLookup source) {
                this.lookupContent = filterSerializable(source.lookup.lookupAll(Object.class));
            }

            private static Object[] filterSerializable(Collection<?> objects) {
                List<Object> result = new ArrayList<>(objects.size());
                for (Object obj: objects) {
                    if (obj instanceof Serializable) {
                        result.add(obj);
                    }
                }
                return result.toArray();
            }

            private Object readResolve() throws ObjectStreamException {
                return new SerializableLookup(Lookups.fixed(lookupContent));
            }
        }
    }
}
