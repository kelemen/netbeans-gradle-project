package org.netbeans.gradle.project;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Utilities;

public final class GradleBinaryForSourceQuery implements BinaryForSourceQueryImplementation {
    private static final Logger LOGGER = Logger.getLogger(GradleSourceForBinaryQuery.class.getName());

    private static final URL[] NO_ROOTS = new URL[0];

    private final ConcurrentMap<FileObject, BinaryForSourceQuery.Result> cache;
    private final NbGradleProject project;
    private final ChangeSupport changes;

    public GradleBinaryForSourceQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.changes = new ChangeSupport(project);
        this.cache = new ConcurrentHashMap<FileObject, BinaryForSourceQuery.Result>();
    }

    private static SourceType getSourceRootType(IdeaModule module, FileObject root) {
        for (IdeaContentRoot contentRoot: module.getContentRoots()) {
            for (IdeaSourceDirectory ideaSrcDir: contentRoot.getSourceDirectories()) {
                FileObject src = FileUtil.toFileObject(ideaSrcDir.getDirectory());
                if (src != null && FileUtil.getRelativePath(src, root) != null) {
                    return SourceType.NORMAL;
                }
            }

            for (IdeaSourceDirectory ideaSrcDir: contentRoot.getTestDirectories()) {
                FileObject src = FileUtil.toFileObject(ideaSrcDir.getDirectory());
                if (src != null && FileUtil.getRelativePath(src, root) != null) {
                    return SourceType.TEST;
                }
            }
        }

        return SourceType.UNKNOWN;
    }

    private static URL[] getBinariesOfModule(NbProjectModel projectModel, IdeaModule module) {
        File moduleDir = NbProjectModelUtils.getIdeaModuleDir(projectModel, module);
        try {
            URL url = Utilities.toURI(new File(moduleDir, GradleClassPathProvider.RELATIVE_OUTPUT_PATH)).toURL();
            return new URL[]{url};
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.WARNING, "Cannot convert to URL: " + moduleDir, ex);
        }

        return NO_ROOTS;
    }

    private static URL[] getTestBinariesOfModule(NbProjectModel projectModel, IdeaModule module) {
        File moduleDir = NbProjectModelUtils.getIdeaModuleDir(projectModel, module);
        try {
            URL url = Utilities.toURI(new File(moduleDir, GradleClassPathProvider.RELATIVE_TEST_OUTPUT_PATH)).toURL();
            return new URL[]{url};
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.WARNING, "Cannot convert to URL: " + moduleDir, ex);
        }

        return NO_ROOTS;
    }

    private static URL[] tryGetRoots(
            NbProjectModel projectModel, IdeaModule module, FileObject root) {
        SourceType sourceType = getSourceRootType(module, root);
        switch (sourceType) {
            case NORMAL:
                return getBinariesOfModule(projectModel, module);
            case TEST:
                return getTestBinariesOfModule(projectModel, module);
            case UNKNOWN:
                return null;
            default:
                throw new AssertionError(sourceType.name());

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
    public BinaryForSourceQuery.Result findBinaryRoots(URL sourceRoot) {
        fetchSources();

        File sourceRootFile = FileUtil.archiveOrDirForURL(sourceRoot);
        if (sourceRootFile == null) {
            return null;
        }
        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRootFile);
        if (sourceRootObj == null) {
            return null;
        }

        BinaryForSourceQuery.Result result = cache.get(sourceRootObj);
        if (result != null) {
            return result;
        }

        result = new BinaryForSourceQuery.Result() {
            @Override
            public URL[] getRoots() {
                NbProjectModel projectModel = project.tryGetCachedProject();
                if (projectModel == null) {
                    return NO_ROOTS;
                }

                IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
                if (mainModule == null) {
                    return NO_ROOTS;
                }

                URL[] roots = tryGetRoots(projectModel, mainModule, sourceRootObj);
                if (roots != null) {
                    return roots;
                }

                for (IdeaModule dependency: NbProjectModelUtils.getIdeaProjectDependencies(mainModule)) {
                    URL[] depRoots = tryGetRoots(projectModel, dependency, sourceRootObj);
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
        BinaryForSourceQuery.Result prevResult = cache.putIfAbsent(sourceRootObj, result);
        return prevResult != null ? prevResult : result;
    }

    private enum SourceType {
        NORMAL,
        TEST,
        UNKNOWN
    }
}
