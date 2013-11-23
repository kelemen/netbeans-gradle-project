package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.entry.ExtensionLoadResult;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery;
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
    private static LoadedExtension loadExtension(NbGradleProject project, GradleProjectExtensionQuery def) {
        GradleProjectExtension extension = null;

        try {
            extension = def.loadExtensionForProject(project);
            if (extension == null) throw new NullPointerException("def.loadExtensionForProject");

            GradleProjectExtensionDef def2 = createWrappedDef(project, def, extension);
            GradleProjectExtension2 extension2 = createWrappedProjectExtension(project, extension);
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

    private static LoadedExtension loadExtension(NbGradleProject project, GradleProjectExtensionDef def) throws IOException {
        try {
            GradleProjectExtension2 extension = def.createExtension(project);
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

    public static List<LoadedExtension> loadExtensions(NbGradleProject project) throws IOException {
        Lookup defaultLookup = Lookup.getDefault();

        Collection<? extends GradleProjectExtensionQuery> defs1
                = defaultLookup.lookupAll(GradleProjectExtensionQuery.class);

        Collection<? extends GradleProjectExtensionDef> defs2
                = defaultLookup.lookupAll(GradleProjectExtensionDef.class);

        int expectedExtensionCount = defs1.size() + defs2.size();
        List<LoadedExtension> result = new ArrayList<LoadedExtension>(expectedExtensionCount);

        Set<String> alreadyLoaded = CollectionUtils.newHashSet(expectedExtensionCount);
        for (GradleProjectExtensionDef def: defs2) {
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

    private static GradleProjectExtension2 createWrappedProjectExtension(
            final NbGradleProject project,
            final GradleProjectExtension extension) {

        final Lookup permanentLookup = extension.getExtensionLookup();

        return new GradleProjectExtension2() {
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
            public boolean loadFromCache(Object cachedModel) {
                if (cachedModel instanceof Lookup) {
                    extension.modelsLoaded((Lookup)cachedModel);
                }
                else {
                    LOGGER.log(Level.WARNING,
                            "Unexpected cached model: {0} for project {1}",
                            new Object[]{cachedModel, project.getProjectDirectoryAsFile()});
                }
                return true;
            }

            @Override
            public ExtensionLoadResult loadFromModels(Lookup models) {
                Map<File, Lookup> deduced = extension.deduceModelsForProjects(models);
                Map<File, Object> result = CollectionUtils.newHashMap(deduced.size() + 1);
                result.putAll(deduced);
                result.put(project.getProjectDirectoryAsFile(), models);

                extension.modelsLoaded(models);

                return new ExtensionLoadResult(true, result);
            }
        };
    }

    private static GradleProjectExtensionDef createWrappedDef(
            NbGradleProject project,
            final GradleProjectExtensionQuery query,
            final GradleProjectExtension extension) {

        final Lookup lookup = Lookups.singleton(getModelQuery(extension));

        return new GradleProjectExtensionDef() {
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
            public GradleProjectExtension2 createExtension(Project project) throws IOException {
                // This won't really be called, only implemented for completness sake.
                GradleProjectExtension newExtension = query.loadExtensionForProject(project);
                NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
                if (gradleProject == null) {
                    throw new NullPointerException("Expected a Gradle project.");
                }

                return createWrappedProjectExtension(gradleProject, newExtension);
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
