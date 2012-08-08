package org.netbeans.gradle.project;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleSourceForBinaryQuery implements SourceForBinaryQueryImplementation2 {
    // You would think that this is an important query for the debugger to
    // find the source files. Well, no ...
    //
    // The debugger actually does this (in NB 7.2 at least):
    //
    // 1. Collects all the sources from the GlobalPathRegistry.
    // 2. Retrieves whatever classpaths are on the EXECUTE classpath of the
    //    main project (yes, the one which can be selected to written its name
    //    with bold font, even though it was said that setting something as the
    //    main project is only to conveniently build, run etc. from the buttons
    //    on the toolbar)
    // 3. The binary paths collected are translated by this query to source
    //    paths
    // 4. All the source paths which has been collected are checked if they
    //    contain the required source file.
    //
    // Conclusion: This query has little to no importance because you can't
    //    expect the user to select something as the main project just to be
    //    able to debug it (I don't think anyone would expect this behaviour).


    private static final Logger LOGGER = Logger.getLogger(GradleSourceForBinaryQuery.class.getName());

    private static final FileObject[] NO_ROOTS = new FileObject[0];

    private final ConcurrentMap<FileObject, SourceForBinaryQueryImplementation2.Result> cache;
    private final NbGradleProject project;
    private final ChangeSupport changes;

    public GradleSourceForBinaryQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.changes = new ChangeSupport(project);
        this.cache = new ConcurrentHashMap<FileObject, SourceForBinaryQueryImplementation2.Result>();
    }

    private static BinaryType getBinaryRootType(
            NbProjectModel projectModel, IdeaModule module, FileObject root) {
        File moduleDir = NbProjectModelUtils.getIdeaModuleDir(projectModel, module);
        FileObject moduleDirObj = FileUtil.toFileObject(moduleDir);
        if (moduleDirObj != null) {
            FileObject normalPath = moduleDirObj.getFileObject(GradleClassPathProvider.RELATIVE_OUTPUT_PATH);
            if (normalPath != null && FileUtil.getRelativePath(normalPath, root) != null) {
                return BinaryType.NORMAL;
            }

            FileObject testPath = moduleDirObj.getFileObject(GradleClassPathProvider.RELATIVE_TEST_OUTPUT_PATH);
            if (testPath != null && FileUtil.getRelativePath(testPath, root) != null) {
                return BinaryType.TEST;
            }
        }

        // We don't have sources for any external dependencies.
        return BinaryType.UNKNOWN;
    }

    private static FileObject[] getSourcesOfModule(IdeaModule module) {
        List<FileObject> result = new LinkedList<FileObject>();
        for (IdeaContentRoot contentRoot: module.getContentRoots()) {
            for (IdeaSourceDirectory srcDir: contentRoot.getSourceDirectories()) {
                if (!NbProjectModelUtils.isResourcePath(srcDir)) {
                    FileObject dir = FileUtil.toFileObject(srcDir.getDirectory());
                    if (dir != null) {
                        result.add(dir);
                    }
                }
            }
        }
        return result.toArray(NO_ROOTS);
    }

    private static FileObject[] getTestSourcesOfModule(IdeaModule module) {
        List<FileObject> result = new LinkedList<FileObject>();
        for (IdeaContentRoot contentRoot: module.getContentRoots()) {
            for (IdeaSourceDirectory srcDir: contentRoot.getTestDirectories()) {
                if (!NbProjectModelUtils.isResourcePath(srcDir)) {
                    FileObject dir = FileUtil.toFileObject(srcDir.getDirectory());
                    if (dir != null) {
                        result.add(dir);
                    }
                }
            }
        }
        return result.toArray(NO_ROOTS);
    }

    private static FileObject[] tryGetRoots(
            NbProjectModel projectModel, IdeaModule module, FileObject root) {
        BinaryType binaryType = getBinaryRootType(projectModel, module, root);
        switch (binaryType) {
            case NORMAL:
                return getSourcesOfModule(module);
            case TEST:
                return getTestSourcesOfModule(module);
            case UNKNOWN:
                return null;
            default:
                throw new AssertionError(binaryType.name());

        }
    }

    private void fetchSources() {
        if (project.tryGetCachedProject() == null) {
            if (SwingUtilities.isEventDispatchThread()) {
                NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        fetchSources();
                    }
                });
            }
            else {
                project.loadProject();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        changes.fireChange();
                    }
                });
            }
        }
    }

    @Override
    public SourceForBinaryQueryImplementation2.Result findSourceRoots2(URL binaryRoot) {
        fetchSources();

        File binaryRootFile = FileUtil.archiveOrDirForURL(binaryRoot);
        if (binaryRootFile == null) {
            return null;
        }
        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRootFile);
        if (binaryRootObj == null) {
            return null;
        }

        SourceForBinaryQueryImplementation2.Result result = cache.get(binaryRootObj);
        if (result != null) {
            return result;
        }

        result = new SourceForBinaryQueryImplementation2.Result() {
            @Override
            public boolean preferSources() {
                return getRoots().length > 0;
            }

            @Override
            public FileObject[] getRoots() {
                NbProjectModel projectModel = project.tryGetCachedProject();
                if (projectModel == null) {
                    return NO_ROOTS;
                }

                IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
                if (mainModule == null) {
                    return NO_ROOTS;
                }

                FileObject[] roots = tryGetRoots(projectModel, mainModule, binaryRootObj);
                if (roots != null) {
                    return roots;
                }

                for (IdeaModule dependency: NbProjectModelUtils.getIdeaProjectDependencies(mainModule)) {
                    FileObject[] depRoots = tryGetRoots(projectModel, dependency, binaryRootObj);
                    if (depRoots != null) {
                        return depRoots;
                    }
                }

                return NO_ROOTS;
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                changes.addChangeListener(listener);
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                changes.removeChangeListener(listener);
            }

            @Override
            public String toString() {
                return Arrays.toString(getRoots());
            }
        };
        SourceForBinaryQueryImplementation2.Result prevResult = cache.putIfAbsent(binaryRootObj, result);
        return prevResult != null ? prevResult : result;
    }

    @Override
    public SourceForBinaryQuery.Result findSourceRoots(URL binaryRoot) {
        return findSourceRoots2(binaryRoot);
    }

    private enum BinaryType {
        NORMAL,
        TEST,
        UNKNOWN
    }
}
