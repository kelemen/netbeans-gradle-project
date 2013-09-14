package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.model.FetchedBuildModels;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.FetchedProjectModels;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleProjectInfoQuery;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectExtensionRef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class NbGradle18ModelLoader implements NbModelLoader {
    private final OperationInitializer setup;

    public NbGradle18ModelLoader(OperationInitializer setup) {
        if (setup == null) throw new NullPointerException("setup");

        this.setup = setup;
    }

    private GenericModelFetcher getModelFetcher(List<ProjectExtensionRef> extensionRefs) {
        Map<Object, GradleBuildInfoQuery<?>> buildInfoRequests = Collections.emptyMap();
        Map<Object, GradleProjectInfoQuery<?>> projectInfoRequests = Collections.emptyMap();

        List<Class<?>> models = new LinkedList<Class<?>>();
        for (ProjectExtensionRef extensionRef: extensionRefs) {
            GradleProjectExtension extension = extensionRef.getExtension();

            Iterable<List<Class<?>>> extensionModels = extension.getGradleModels();
            for (List<Class<?>> requestList: extensionModels) {
                models.addAll(requestList);
            }
        }

        return new GenericModelFetcher(buildInfoRequests, projectInfoRequests, models);
    }

    private Lookup extractNeededModels(GradleProjectExtension extension, FetchedProjectModels models) {
        Iterable<List<Class<?>>> neededModels = extension.getGradleModels();
        if (!neededModels.iterator().hasNext()) {
            return Lookup.EMPTY;
        }

        Map<Class<?>, Object> toolingModels = models.getToolingModels();

        List<Object> foundModels = new LinkedList<Object>();
        for (List<Class<?>> modelList: neededModels) {
            for (Class<?> modelClass: modelList) {
                Object modelValue = toolingModels.get(modelClass);
                if (modelValue != null) {
                    foundModels.add(modelValue);
                    break;
                }
            }
        }

        return Lookups.fixed(foundModels.toArray());
    }

    private NbGradleModel getNBModel(
            FetchedBuildModels buildModels,
            FetchedProjectModels models,
            List<ProjectExtensionRef> extensionRefs) {

        NbGradleModel result = new NbGradleModel(new NbGradleMultiProjectDef(models.getProjectDef()));
        for (ProjectExtensionRef extensionRef: extensionRefs) {
            Lookup extLookup = extractNeededModels(extensionRef.getExtension(), models);
            result.setModelsForExtension(extensionRef.getName(), extLookup);
        }
        return result;
    }

    private NbGradleModel getDefaultNBModels(
            FetchedModels fetchedModels,
            List<ProjectExtensionRef> extensionRefs) {

        return getNBModel(
                fetchedModels.getBuildModels(),
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

        NbGradleModel mainModel = getDefaultNBModels(fetchedModels, extensionRefs);

        FetchedBuildModels buildModels = fetchedModels.getBuildModels();
        Collection<FetchedProjectModels> otherFetchedModels = fetchedModels.getOtherProjectModels();

        List<NbGradleModel> otherModels = new ArrayList<NbGradleModel>(otherFetchedModels.size());
        for (FetchedProjectModels fetchedProjectModel: otherFetchedModels) {
            NbGradleModel otherModel = getNBModel(buildModels, fetchedProjectModel, extensionRefs);
            otherModels.add(otherModel);
        }

        return new Result(mainModel, otherModels);
    }
}
