package org.netbeans.gradle.project.model;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

public final class NbModelUtils {
    private static void getAllDependencies(
            NbGradleModule module,
            NbDependencyType type,
            Collection<NbDependency> toAdd,
            Set<String> toSkip) {
        if (!toSkip.add(module.getUniqueName())) {
            return;
        }

        for (NbDependency dependency: module.getDependencies(NbDependencyType.COMPILE).getAllDependencies()) {
            toAdd.add(dependency);
            if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                NbDependencyType recType;
                switch (type) {
                    case RUNTIME:
                        recType = NbDependencyType.RUNTIME;
                        break;
                    case TEST_RUNTIME:
                        recType = NbDependencyType.RUNTIME;
                        break;
                    default:
                        recType = NbDependencyType.COMPILE;
                        break;
                }
                getAllDependencies(moduleDep.getModule(), recType, toAdd, toSkip);
            }
        }

        if (type == NbDependencyType.RUNTIME || type == NbDependencyType.TEST_RUNTIME) {
            for (NbDependency dependency: module.getDependencies(NbDependencyType.RUNTIME).getAllDependencies()) {
                toAdd.add(dependency);
                if (dependency instanceof NbModuleDependency) {
                    NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                    getAllDependencies(moduleDep.getModule(), NbDependencyType.RUNTIME, toAdd, toSkip);
                }
            }
        }

        if (type == NbDependencyType.TEST_COMPILE || type == NbDependencyType.TEST_RUNTIME) {
            for (NbDependency dependency: module.getDependencies(NbDependencyType.TEST_COMPILE).getAllDependencies()) {
                toAdd.add(dependency);
                if (dependency instanceof NbModuleDependency) {
                    NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                    NbDependencyType recType;
                    switch (type) {
                        case TEST_COMPILE:
                            recType = NbDependencyType.COMPILE;
                            break;
                        case TEST_RUNTIME:
                            recType = NbDependencyType.RUNTIME;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    getAllDependencies(moduleDep.getModule(), recType, toAdd, toSkip);
                }
            }
        }

        if (type == NbDependencyType.TEST_RUNTIME) {
            for (NbDependency dependency: module.getDependencies(NbDependencyType.TEST_RUNTIME).getAllDependencies()) {
                toAdd.add(dependency);
                if (dependency instanceof NbModuleDependency) {
                    NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                    getAllDependencies(moduleDep.getModule(), NbDependencyType.RUNTIME, toAdd, toSkip);
                }
            }
        }
    }

    public static Collection<NbDependency> getAllDependencies(
            NbGradleModule module, NbDependencyType type) {
        if (module == null) throw new NullPointerException("module");
        if (type == null) throw new NullPointerException("type");
        if (type == NbDependencyType.OTHER) {
            throw new IllegalArgumentException("Cannot fetch this kind of dependencies: " + type);
        }

        Set<NbDependency> dependencies = new LinkedHashSet<NbDependency>();
        getAllDependencies(module, type, dependencies, new HashSet<String>());

        return dependencies;
    }

    public static Collection<NbGradleModule> getAllModuleDependencies(
            NbGradleModule module, NbDependencyType type) {
        List<NbGradleModule> result = new LinkedList<NbGradleModule>();
        for (NbDependency dependency: getAllDependencies(module, type)) {
            if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                result.add(moduleDep.getModule());
            }
        }
        return result;
    }

    public static Collection<NbGradleModule> getAllModuleDependencies(NbGradleModule module) {
        return getAllModuleDependencies(module, NbDependencyType.TEST_RUNTIME);
    }

    public static File uriToFile(URI uri) {
        if ("file".equals(uri.getScheme())) {
            return Utilities.toFile(uri);
        }
        else {
            return null;
        }
    }

    public static FileObject uriToFileObject(URI uri) {
        File file = uriToFile(uri);
        return file != null ? FileUtil.toFileObject(file) : null;
    }

    private NbModelUtils() {
        throw new AssertionError();
    }
}
