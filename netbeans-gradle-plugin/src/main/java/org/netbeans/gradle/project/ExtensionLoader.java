package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery;
import org.netbeans.gradle.project.api.entry.ParsedModel;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery1;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
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

    @SuppressWarnings("UseSpecificCatch")
    private static LoadedExtension loadExtension(
            NbGradleProject project,
            GradleProjectExtensionQuery def) {

        GradleProjectExtension extension = null;

        try {
            extension = def.loadExtensionForProject(project);
            if (extension == null) throw new NullPointerException("def.loadExtensionForProject");

            GradleProjectExtensionDef<Lookup> def2 = createWrappedDef(project, def, extension);
            GradleProjectExtension2<Lookup> extension2 = createWrappedProjectExtension(extension);
            return new LoadedExtension(def2, extension2);
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

    private static <ModelType> LoadedExtension loadExtension(
            NbGradleProject project,
            GradleProjectExtensionDef<ModelType> def) throws IOException {

        try {
            GradleProjectExtension2<ModelType> extension = def.createExtension(project);
            return new LoadedExtension(def, extension);
        } catch (Throwable ex) {
            LOGGER.log(levelFromException(ex),
                    "Failed to load extension: " + def.getName() + " for project " + project.getProjectDirectory(),
                    ex);
            return null;
        }
    }

    private static void tryAddExtension(
            Object sourceDef,
            LoadedExtension extension,
            List<LoadedExtension> result,
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

    public static List<LoadedExtension> loadExtensions(NbGradleProject project) throws IOException {
        Lookup defaultLookup = Lookup.getDefault();

        Collection<? extends GradleProjectExtensionQuery> defs1
                = defaultLookup.lookupAll(GradleProjectExtensionQuery.class);

        Collection<? extends GradleProjectExtensionDef<?>> defs2
                = defaultLookup.lookupAll(defClass());

        int expectedExtensionCount = defs1.size() + defs2.size();
        List<LoadedExtension> result = new ArrayList<LoadedExtension>(expectedExtensionCount);

        Set<String> alreadyLoaded = CollectionUtils.newHashSet(expectedExtensionCount);
        for (GradleProjectExtensionDef<?> def: defs2) {
            LoadedExtension loadedExtension = loadExtension(project, def);
            tryAddExtension(def, loadedExtension, result, alreadyLoaded);
        }

        for (GradleProjectExtensionQuery def: defs1) {
            LoadedExtension loadedExtension = loadExtension(project, def);
            tryAddExtension(def, loadedExtension, result, alreadyLoaded);
        }

        // TODO: Display balloon if there were duplicate extensions.

        return result;
    }

    private static GradleModelDefQuery1 getModelQuery(GradleProjectExtension extension) {
        List<Class<?>> allModels = new LinkedList<Class<?>>();
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

    private static GradleProjectExtension2<Lookup> createWrappedProjectExtension(
            final GradleProjectExtension extension) {

        final Lookup permanentLookup = extension.getExtensionLookup();

        return new GradleProjectExtension2<Lookup>() {
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
            public void activateExtension(Lookup parsedModel) {
                extension.modelsLoaded(parsedModel);
            }

            @Override
            public void deactivateExtension() {
                extension.modelsLoaded(Lookup.EMPTY);
            }
        };
    }

    private static GradleProjectExtensionDef<Lookup> createWrappedDef(
            final NbGradleProject project,
            final GradleProjectExtensionQuery query,
            final GradleProjectExtension extension) {

        final Lookup lookup = Lookups.singleton(getModelQuery(extension));

        return new GradleProjectExtensionDef<Lookup>() {
            @Override
            public String getName() {
                return extension.getExtensionName();
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
            public Class<Lookup> getModelType() {
                return Lookup.class;
            }

            @Override
            public ParsedModel<Lookup> parseModel(Lookup retrievedModels) {
                Map<File, Lookup> deduced = extension.deduceModelsForProjects(retrievedModels);

                File projectDir = project.getProjectDirectoryAsFile();
                Lookup mainModels = deduced.get(projectDir);
                if (mainModels != null) {
                    deduced = new HashMap<File, Lookup>(deduced);
                    deduced.remove(projectDir);
                }

                return new ParsedModel<Lookup>(mainModels, deduced);
            }

            @Override
            public GradleProjectExtension2<Lookup> createExtension(Project project) throws IOException {
                // This won't really be called, only implemented for completness sake.
                GradleProjectExtension newExtension = query.loadExtensionForProject(project);
                if (newExtension == null) {
                    throw new NullPointerException("GradleProjectExtensionQuery.loadExtensionForProject");
                }
                return createWrappedProjectExtension(newExtension);
            }

            @Override
            public Set<String> getSuppressedExtensions() {
                return Collections.emptySet();
            }
        };
    }

    private ExtensionLoader() {
        throw new AssertionError();
    }
}
