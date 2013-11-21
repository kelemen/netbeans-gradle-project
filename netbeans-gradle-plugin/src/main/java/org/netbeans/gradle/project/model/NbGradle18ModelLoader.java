package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.FetchedProjectModels;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.MultiKey;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectExtensionRef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.openide.util.lookup.Lookups;

public final class NbGradle18ModelLoader implements NbModelLoader {
    private static final Logger LOGGER = Logger.getLogger(NbGradle18ModelLoader.class.getName());

    private final OperationInitializer setup;

    public NbGradle18ModelLoader(OperationInitializer setup) {
        if (setup == null) throw new NullPointerException("setup");

        this.setup = setup;
    }

    private CustomModelQuery getCustomModelQuery(GradleProjectExtension extension) {
        CustomModelQuery result = extension.getExtensionLookup().lookup(CustomModelQuery.class);
        if (result != null) {
            return result;
        }

        Collection<Class<?>> extensionModels = new LinkedList<Class<?>>();
        for (List<Class<?>> fallbackList: extension.getGradleModels()) {
            extensionModels.addAll(fallbackList);
        }

        final Collection<Class<?>> resultModels = Collections.unmodifiableCollection(extensionModels);
        return new CustomModelQuery() {
            @Override
            public Collection<Class<?>> getProjectModels() {
                return resultModels;
            }

            @Override
            public Map<Object, GradleBuildInfoQuery<?>> getBuildInfoQueries() {
                return Collections.emptyMap();
            }

            @Override
            public Map<Object, GradleProjectInfoQuery<?>> getProjectInfoQueries() {
                return Collections.emptyMap();
            }
        };
    }

    private GenericModelFetcher getModelFetcher(List<ProjectExtensionRef> extensionRefs) {
        // TODO: Exploit the fact that keys map to list of queries.
        //   That is, for a single extension, a single entry is required.

        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfoRequests
                = new HashMap<Object, List<GradleBuildInfoQuery<?>>>();

        Map<Object, List<GradleProjectInfoQuery<?>>> projectInfoRequests
                = new HashMap<Object, List<GradleProjectInfoQuery<?>>>();

        List<Class<?>> models = new LinkedList<Class<?>>();
        for (ProjectExtensionRef extensionRef: extensionRefs) {
            GradleProjectExtension extension = extensionRef.getExtension();
            String extensionName = extensionRef.getName();

            CustomModelQuery extensionNeeds = getCustomModelQuery(extension);

            models.addAll(extensionNeeds.getProjectModels());

            for (Map.Entry<Object, GradleBuildInfoQuery<?>> entry: extensionNeeds.getBuildInfoQueries().entrySet()) {
                Object key = entry.getKey();
                GradleBuildInfoQuery<?> value = entry.getValue();

                buildInfoRequests.put(
                        MultiKey.create(extensionName, key),
                        Collections.<GradleBuildInfoQuery<?>>singletonList(value));
            }

            for (Map.Entry<Object, GradleProjectInfoQuery<?>> entry: extensionNeeds.getProjectInfoQueries().entrySet()) {
                Object key = entry.getKey();
                GradleProjectInfoQuery<?> value = entry.getValue();

                projectInfoRequests.put(
                        MultiKey.create(extensionName, key),
                        Collections.<GradleProjectInfoQuery<?>>singletonList(value));
            }
        }

        return new GenericModelFetcher(buildInfoRequests, projectInfoRequests, models);
    }

    private void extractNeededModels(
            GradleProjectExtension extension,
            FetchedProjectModels models,
            List<Object> result) {

        Map<Class<?>, Object> toolingModels = models.getToolingModels();

        for (List<Class<?>> modelList: extension.getGradleModels()) {
            for (Class<?> modelClass: modelList) {
                Object modelValue = toolingModels.get(modelClass);
                if (modelValue != null) {
                    result.add(modelValue);
                    break;
                }
            }
        }
    }

    private static GradleBuildInfo tryGetBuildInfo(AllBuildInfos allInfos, File projectDir, String extensionName) {
        ExtensionBuildInfos extensionInfos = allInfos.getForExtension(extensionName);
        if (extensionInfos == null) {
            LOGGER.log(Level.WARNING,
                    "Cannot find ExtensionBuildInfos for ({0}, {1})",
                    new Object[]{projectDir, extensionName});
            return null;
        }

        return extensionInfos.tryGetBuildInfo(projectDir);
    }

    private NbGradleModel getNBModel(
            AllBuildInfos allInfos,
            FetchedProjectModels models,
            List<ProjectExtensionRef> extensionRefs) {

        File projectDir = models.getProjectDef().getMainProject().getGenericProperties().getProjectDir();

        NbGradleModel result = new NbGradleModel(new NbGradleMultiProjectDef(models.getProjectDef()));
        for (ProjectExtensionRef extensionRef: extensionRefs) {
            String name = extensionRef.getName();

            List<Object> lookupContent = new LinkedList<Object>();

            extractNeededModels(extensionRef.getExtension(), models, lookupContent);
            allInfos.getForExtension(name);

            GradleBuildInfo buildInfo = tryGetBuildInfo(allInfos, projectDir, name);
            if (buildInfo != null) {
                lookupContent.add(buildInfo);
            }

            result.setModelsForExtension(name, Lookups.fixed(lookupContent.toArray()));
        }
        return result;
    }

    private NbGradleModel getDefaultNBModels(
            FetchedModels fetchedModels,
            AllBuildInfos allInfos,
            List<ProjectExtensionRef> extensionRefs) {

        return getNBModel(
                allInfos,
                fetchedModels.getDefaultProjectModels(),
                extensionRefs);
    }

    @Override
    public Result loadModels(
            NbGradleProject project,
            ProjectConnection connection,
            ProgressHandle progress) throws IOException {

        List<ProjectExtensionRef> extensionRefs = project.getExtensionRefs();

        GenericModelFetcher modelFetcher = getModelFetcher(extensionRefs);
        FetchedModels fetchedModels = modelFetcher.getModels(connection, setup);
        AllBuildInfos allInfos = new AllBuildInfos(fetchedModels, extensionRefs);

        NbGradleModel mainModel = getDefaultNBModels(fetchedModels, allInfos, extensionRefs);

        Collection<FetchedProjectModels> otherFetchedModels = fetchedModels.getOtherProjectModels();

        List<NbGradleModel> otherModels = new ArrayList<NbGradleModel>(otherFetchedModels.size());
        for (FetchedProjectModels fetchedProjectModel: otherFetchedModels) {
            NbGradleModel otherModel = getNBModel(allInfos, fetchedProjectModel, extensionRefs);
            otherModels.add(otherModel);
        }

        return new Result(mainModel, otherModels);
    }

    private static final class ExtensionBuildInfos {
        private final GradleBuildInfoOfExtension extBuildInfo;

        public ExtensionBuildInfos(GradleBuildInfoOfExtension extBuildInfo) {
            this.extBuildInfo = extBuildInfo;
        }

        public GradleBuildInfoOfExtension getDefaultBuildInfo() {
            return extBuildInfo;
        }

        public GradleBuildInfo tryGetBuildInfo(File projectDir) {
            return extBuildInfo.tryGetViewOfOtherProject(projectDir);
        }
    }

    private static final class AllBuildInfos {
        private final Map<String, ExtensionBuildInfos> infos;

        public AllBuildInfos(FetchedModels models, Collection<ProjectExtensionRef> extensions) {
            this.infos = new HashMap<String, NbGradle18ModelLoader.ExtensionBuildInfos>(2 * extensions.size());
            for (ProjectExtensionRef extRef: extensions) {
                String name = extRef.getName();

                GradleBuildInfoOfExtension extBuildInfo = new GradleBuildInfoOfExtension(name, models);
                infos.put(name, new ExtensionBuildInfos(extBuildInfo));
            }
        }

        public ExtensionBuildInfos getForExtension(String name) {
            return infos.get(name);
        }
    }
}
