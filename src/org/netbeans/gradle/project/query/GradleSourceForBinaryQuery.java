package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.model.NbDependencyGroup;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.model.NbOutput;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.gradle.project.model.NbUriDependency;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleSourceForBinaryQuery
implements
        SourceForBinaryQueryImplementation2,
        ProjectInitListener {
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
            NbGradleModule module, FileObject root) {
        NbOutput output = module.getProperties().getOutput();

        FileObject normalPath = FileUtil.toFileObject(output.getBuildDir());
        if (normalPath != null && FileUtil.getRelativePath(normalPath, root) != null) {
            return BinaryType.NORMAL;
        }

        FileObject testPath = FileUtil.toFileObject(output.getTestBuildDir());
        if (testPath != null && FileUtil.getRelativePath(testPath, root) != null) {
            return BinaryType.TEST;
        }

        return BinaryType.UNKNOWN;
    }

    private static FileObject[] getSourcesOfModule(NbGradleModule module) {
        List<FileObject> result = module.getSources(NbSourceType.SOURCE).getFileObjects();
        return result.toArray(NO_ROOTS);
    }

    private static FileObject[] getTestSourcesOfModule(NbGradleModule module) {
        List<FileObject> result = module.getSources(NbSourceType.TEST_SOURCE).getFileObjects();
        return result.toArray(NO_ROOTS);
    }

    private static FileObject[] getDependencySources(NbGradleModule module, FileObject root) {
        for (NbDependencyGroup dependency: module.getDependencies().values()) {
            for (NbUriDependency uriDep: dependency.getUriDependencies()) {
                URI srcUri = uriDep.getSrcUri();
                if (srcUri != null) {
                    URI uri = uriDep.getUri();
                    FileObject depRoot = NbModelUtils.uriToFileObject(uri);
                    if (root.equals(depRoot)) {
                        FileObject src = NbModelUtils.uriToFileObject(srcUri);
                        if (src != null) {
                            return new FileObject[]{src};
                        }
                    }
                }
            }
        }
        return null;
    }

    private static FileObject[] tryGetRoots(
            NbGradleModule module, FileObject root) {
        BinaryType binaryType = getBinaryRootType(module, root);
        switch (binaryType) {
            case NORMAL:
                return getSourcesOfModule(module);
            case TEST:
                return getTestSourcesOfModule(module);
            case UNKNOWN:
                return getDependencySources(module, root);
            default:
                throw new AssertionError(binaryType.name());

        }
    }

    private void onModelChange() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                changes.fireChange();
            }
        });
    }

    @Override
    public void onInitProject() {
        project.addModelChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                onModelChange();
            }
        });
        // This is not called because it would trigger the loading of the
        // project even if it just shown in the project open dialog.
        // Although it should be called to ensure correct behaviour in every
        // case.
        // onModelChange();
    }

    @Override
    public SourceForBinaryQueryImplementation2.Result findSourceRoots2(URL binaryRoot) {
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
                NbGradleModel projectModel = project.getCurrentModel();
                NbGradleModule mainModule = projectModel.getMainModule();

                FileObject[] roots = tryGetRoots(mainModule, binaryRootObj);
                if (roots != null) {
                    return roots;
                }

                for (NbGradleModule dependency: NbModelUtils.getAllModuleDependencies(mainModule)) {
                    FileObject[] depRoots = tryGetRoots(dependency, binaryRootObj);
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
