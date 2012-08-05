package org.netbeans.gradle.project;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.gradle.tooling.model.idea.IdeaModule;
import org.netbeans.spi.java.queries.MultipleRootsUnitTestForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class NbUnitTestFinder implements MultipleRootsUnitTestForSourceQueryImplementation {
    private static final URL[] NO_URL = new URL[0];

    private final NbGradleProject project;

    public NbUnitTestFinder(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    private static boolean hasSource(IdeaModule module, FileObject source) {
        for (File srcDirFile: GradleProjectSources.getSourceRoots(module).get(SourceFileType.SOURCE)) {
            FileObject srcDir = FileUtil.toFileObject(srcDirFile);
            if (srcDir != null && FileUtil.getRelativePath(srcDir, source) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTest(IdeaModule module, FileObject source) {
        for (File srcDirFile: GradleProjectSources.getSourceRoots(module).get(SourceFileType.TEST_SOURCE)) {
            FileObject srcDir = FileUtil.toFileObject(srcDirFile);
            if (srcDir != null && FileUtil.getRelativePath(srcDir, source) != null) {
                return true;
            }
        }
        return false;
    }

    private static URL[] getSourceRoots(IdeaModule module) {
        List<URL> result = new LinkedList<URL>();
        for (File srcDirFile: GradleProjectSources.getSourceRoots(module).get(SourceFileType.SOURCE)) {
            result.add(FileUtil.urlForArchiveOrDir(srcDirFile));
        }

        return result.toArray(NO_URL);
    }

    private static URL[] getTestRoots(IdeaModule module) {
        List<URL> result = new LinkedList<URL>();
        for (File srcDirFile: GradleProjectSources.getSourceRoots(module).get(SourceFileType.TEST_SOURCE)) {
            result.add(FileUtil.urlForArchiveOrDir(srcDirFile));
        }


        return result.toArray(NO_URL);
    }

    @Override
    public URL[] findUnitTests(FileObject source) {
        NbProjectModel projectModel = project.tryGetCachedProject();
        if (projectModel == null) {
            return null;
        }

        IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
        if (hasSource(mainModule, source)) {
            return getTestRoots(mainModule);
        }

        for (IdeaModule module: NbProjectModelUtils.getIdeaProjectDependencies(mainModule)) {
            if (hasSource(module, source)) {
                return getTestRoots(module);
            }
        }

        return null;
    }

    @Override
    public URL[] findSources(FileObject unitTest) {
        NbProjectModel projectModel = project.tryGetCachedProject();
        if (projectModel == null) {
            return null;
        }

        IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
        if (hasTest(mainModule, unitTest)) {
            return getSourceRoots(mainModule);
        }

        for (IdeaModule module: NbProjectModelUtils.getIdeaProjectDependencies(mainModule)) {
            if (hasTest(module, unitTest)) {
                return getSourceRoots(module);
            }
        }

        return null;
    }
}
