package org.netbeans.gradle.project.model;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

public final class NbModelUtils {
    public static boolean isFullyQualifiedName(String name) {
        return name.startsWith(":");
    }

    public static List<String> getNameParts(String name) {
        String[] namePartsArray = name.split(Pattern.quote(":"));
        List<String> result = new ArrayList<String>(namePartsArray.length);
        for (String namePart: namePartsArray) {
            if (!namePart.isEmpty()) {
                result.add(namePart);
            }
        }
        if (result.isEmpty()) {
            return Collections.singletonList("");
        }
        else {
            return result;
        }
    }

    private static NbGradleModule tryGetChildModule(NbGradleModule module, List<String> nameParts) {
        NbGradleModule currentModule = module;
        for (String name: nameParts) {
            NbGradleModule foundModule = null;
            for (NbGradleModule child: currentModule.getChildren()) {
                if (name.equals(child.getName())) {
                    foundModule = child;
                    break;
                }
            }
            if (foundModule == null) {
                return null;
            }
            currentModule = foundModule;
        }
        return currentModule;
    }

    public static NbGradleModule lookupModuleByName(NbGradleModule refModule, String name) {
        if (refModule == null) throw new NullPointerException("refModule");
        if (name == null) throw new NullPointerException("name");

        List<String> nameParts = getNameParts(name);
        if (isFullyQualifiedName(name)) {
            List<String> refNameParts = refModule.getProperties().getNameParts();
            int refNamePartsSize = refNameParts.size();
            if (refNamePartsSize > nameParts.size()) {
                // In this case name cannot refer to refModule or one of its
                // child modules, we need to do complete look-up in refModule's
                // dependencies.
                for (NbGradleModule dependency: getAllModuleDependencies(refModule)) {
                    if (refNameParts.equals(refModule.getProperties().getNameParts())) {
                        return dependency;
                    }
                }
                return null;
            }

            boolean unrelatedModule;
            if (refNamePartsSize <= nameParts.size()) {
                unrelatedModule = false;
                for (int i = 0; i < refNamePartsSize; i++) {
                    if (!refNameParts.get(i).equals(nameParts.get(i))) {
                        unrelatedModule = true;
                        break;
                    }
                }
            }
            else {
                unrelatedModule = true;
            }

            if (unrelatedModule) {
                // In this case, name cannot refer to refModule or one of its
                // child modules, we need to do complete look-up in refModule's
                // dependencies.
                for (NbGradleModule dependency: getAllModuleDependencies(refModule)) {
                    if (refNameParts.equals(refModule.getProperties().getNameParts())) {
                        return dependency;
                    }
                }
                return null;
            }
            else {
                return tryGetChildModule(refModule, nameParts.subList(refNamePartsSize, nameParts.size()));
            }
        }
        else {
            return tryGetChildModule(refModule, nameParts);
        }
    }

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

    public static Collection<NbDependency> getAllDependencies(NbGradleModule module) {
        return getAllDependencies(module, NbDependencyType.TEST_RUNTIME);
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
