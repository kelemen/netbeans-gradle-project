package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModelUtils;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbSourceType;
import org.netbeans.spi.java.queries.MultipleRootsUnitTestForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleUnitTestFinder implements MultipleRootsUnitTestForSourceQueryImplementation {
    private static final URL[] NO_URL = new URL[0];

    private final JavaExtension javaExt;

    public GradleUnitTestFinder(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
    }

    private static boolean hasSource(NbJavaModule module, FileObject source) {
        for (FileObject srcDir: module.getSources(NbSourceType.SOURCE).getFileObjects()) {
            if (FileUtil.getRelativePath(srcDir, source) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTest(NbJavaModule module, FileObject source) {
        for (FileObject srcDir: module.getSources(NbSourceType.TEST_SOURCE).getFileObjects()) {
            if (FileUtil.getRelativePath(srcDir, source) != null) {
                return true;
            }
        }
        return false;
    }

    private static URL[] getSourceRoots(NbJavaModule module) {
        List<URL> result = new LinkedList<URL>();
        for (File srcDirFile: module.getSources(NbSourceType.SOURCE).getFiles()) {
            result.add(FileUtil.urlForArchiveOrDir(srcDirFile));
        }

        return result.toArray(NO_URL);
    }

    private static URL[] getTestRoots(NbJavaModule module) {
        List<URL> result = new LinkedList<URL>();
        for (File srcDirFile: module.getSources(NbSourceType.TEST_SOURCE).getFiles()) {
            result.add(FileUtil.urlForArchiveOrDir(srcDirFile));
        }


        return result.toArray(NO_URL);
    }

    @Override
    public URL[] findUnitTests(FileObject source) {
        NbJavaModel projectModel = javaExt.getCurrentModel();

        NbJavaModule mainModule = projectModel.getMainModule();
        if (hasSource(mainModule, source)) {
            return getTestRoots(mainModule);
        }

        for (NbJavaModule module: NbJavaModelUtils.getAllModuleDependencies(mainModule)) {
            if (hasSource(module, source)) {
                return getTestRoots(module);
            }
        }

        return null;
    }

    @Override
    public URL[] findSources(FileObject unitTest) {
        NbJavaModel projectModel = javaExt.getCurrentModel();

        NbJavaModule mainModule = projectModel.getMainModule();
        if (hasTest(mainModule, unitTest)) {
            return getSourceRoots(mainModule);
        }

        for (NbJavaModule module: NbJavaModelUtils.getAllModuleDependencies(mainModule)) {
            if (hasTest(module, unitTest)) {
                return getSourceRoots(module);
            }
        }

        return null;
    }
}
