package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.spi.java.queries.MultipleRootsUnitTestForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleUnitTestFinder implements MultipleRootsUnitTestForSourceQueryImplementation {
    private static final URL[] NO_URL = new URL[0];

    private final NbGradleProject project;

    public GradleUnitTestFinder(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    private static boolean hasSource(NbGradleModule module, FileObject source) {
        for (FileObject srcDir: module.getSources(NbSourceType.SOURCE).getFileObjects()) {
            if (FileUtil.getRelativePath(srcDir, source) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTest(NbGradleModule module, FileObject source) {
        for (FileObject srcDir: module.getSources(NbSourceType.TEST_SOURCE).getFileObjects()) {
            if (FileUtil.getRelativePath(srcDir, source) != null) {
                return true;
            }
        }
        return false;
    }

    private static URL[] getSourceRoots(NbGradleModule module) {
        List<URL> result = new LinkedList<URL>();
        for (File srcDirFile: module.getSources(NbSourceType.SOURCE).getFiles()) {
            result.add(FileUtil.urlForArchiveOrDir(srcDirFile));
        }

        return result.toArray(NO_URL);
    }

    private static URL[] getTestRoots(NbGradleModule module) {
        List<URL> result = new LinkedList<URL>();
        for (File srcDirFile: module.getSources(NbSourceType.TEST_SOURCE).getFiles()) {
            result.add(FileUtil.urlForArchiveOrDir(srcDirFile));
        }


        return result.toArray(NO_URL);
    }

    @Override
    public URL[] findUnitTests(FileObject source) {
        NbGradleModel projectModel = project.getCurrentModel();

        NbGradleModule mainModule = projectModel.getMainModule();
        if (hasSource(mainModule, source)) {
            return getTestRoots(mainModule);
        }

        for (NbGradleModule module: NbModelUtils.getAllModuleDependencies(mainModule)) {
            if (hasSource(module, source)) {
                return getTestRoots(module);
            }
        }

        return null;
    }

    @Override
    public URL[] findSources(FileObject unitTest) {
        NbGradleModel projectModel = project.getCurrentModel();

        NbGradleModule mainModule = projectModel.getMainModule();
        if (hasTest(mainModule, unitTest)) {
            return getSourceRoots(mainModule);
        }

        for (NbGradleModule module: NbModelUtils.getAllModuleDependencies(mainModule)) {
            if (hasTest(module, unitTest)) {
                return getSourceRoots(module);
            }
        }

        return null;
    }
}
