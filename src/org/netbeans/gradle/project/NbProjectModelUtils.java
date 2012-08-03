package org.netbeans.gradle.project;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;

public final class NbProjectModelUtils {
    public static boolean isResourcePath(IdeaSourceDirectory srcDir) {
        return srcDir.getDirectory().getName().toLowerCase(Locale.US).startsWith("resource");
    }

    public static Set<IdeaDependency> getIdeaDependencies(IdeaModule module) {
        Set<IdeaDependency> dependencies = new HashSet<IdeaDependency>();
        getIdeaDependencies(module, dependencies);
        return dependencies;
    }

    public static Set<HierarchicalEclipseProject> getEclipseProjectDependencies(HierarchicalEclipseProject project) {
        Set<HierarchicalEclipseProject> dependencies = new HashSet<HierarchicalEclipseProject>();
        getEclipseProjectDependencies(project, dependencies);
        return dependencies;
    }

    private static void getEclipseProjectDependencies(
            HierarchicalEclipseProject project,
            Set<HierarchicalEclipseProject> toAdd) {

        for (EclipseProjectDependency dependency: project.getProjectDependencies()) {
            HierarchicalEclipseProject dependentProject = dependency.getTargetProject();
            if (!toAdd.contains(dependentProject)) {
                toAdd.add(dependentProject);
                getEclipseProjectDependencies(dependentProject, toAdd);
            }
        }
    }

    public static Set<IdeaModule> getIdeaProjectDependencies(IdeaModule module) {
        Set<IdeaModule> dependencies = new HashSet<IdeaModule>();
        getIdeaProjectDependencies(module, dependencies);
        return dependencies;
    }

    private static void getIdeaProjectDependencies(IdeaModule module, Set<IdeaModule> toAdd) {
        for (IdeaDependency dependency: module.getDependencies()) {
            if (dependency instanceof IdeaModuleDependency) {
                IdeaModule dependentModule
                        = ((IdeaModuleDependency)dependency).getDependencyModule();

                if (!toAdd.contains(dependentModule)) {
                    toAdd.add(dependentModule);
                    getIdeaProjectDependencies(dependentModule, toAdd);
                }
            }
        }
    }

    private static void getIdeaDependencies(IdeaModule module, Set<IdeaDependency> toAdd) {
        for (IdeaDependency dependency: module.getDependencies()) {
            if (!toAdd.contains(dependency)) {
                toAdd.add(dependency);
                if (dependency instanceof IdeaModuleDependency) {
                    IdeaModuleDependency moduleDep = (IdeaModuleDependency)dependency;
                    getIdeaDependencies(moduleDep.getDependencyModule(), toAdd);
                }
            }
        }
    }

    public static IdeaModule getMainIdeaModule(NbProjectModel projectModel) {
        EclipseProject eclipseModel = projectModel.getEclipseModel();
        String mainProjectPath = eclipseModel.getGradleProject().getPath();

        for (IdeaModule module: projectModel.getIdeaModel().getModules()) {
            if (mainProjectPath.equals(module.getGradleProject().getPath())) {
                return module;
            }
        }

        return null;
    }

    private NbProjectModelUtils() {
        throw new AssertionError();
    }
}
