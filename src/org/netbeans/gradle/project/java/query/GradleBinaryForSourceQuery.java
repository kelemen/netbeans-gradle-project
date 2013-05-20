package org.netbeans.gradle.project.java.query;

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
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbSourceType;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Utilities;

public final class GradleBinaryForSourceQuery
implements
        BinaryForSourceQueryImplementation,
        JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleSourceForBinaryQuery.class.getName());

    private static final URL[] NO_ROOTS = new URL[0];

    private final ConcurrentMap<FileObject, BinaryForSourceQuery.Result> cache;
    private final JavaExtension javaExt;
    private final ChangeSupport changes;

    public GradleBinaryForSourceQuery(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
        this.cache = new ConcurrentHashMap<FileObject, BinaryForSourceQuery.Result>();

        EventSource eventSource = new EventSource();
        this.changes = new ChangeSupport(eventSource);
        eventSource.init(this.changes);
    }

    private static SourceType getSourceRootType(NbJavaModule module, FileObject root) {
        for (FileObject src: module.getSources(NbSourceType.SOURCE).getFileObjects()) {
            if (FileUtil.getRelativePath(src, root) != null) {
                return SourceType.NORMAL;
            }
        }
        for (FileObject src: module.getSources(NbSourceType.TEST_SOURCE).getFileObjects()) {
            if (FileUtil.getRelativePath(src, root) != null) {
                return SourceType.TEST;
            }
        }

        return SourceType.UNKNOWN;
    }

    private static URL[] getBinariesOfModule(NbJavaModule module) {
        File buildDir = module.getProperties().getOutput().getBuildDir();
        try {
            URL url = Utilities.toURI(buildDir).toURL();
            return new URL[]{url};
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.INFO, "Cannot convert to URL: " + buildDir, ex);
        }

        return NO_ROOTS;
    }

    private static URL[] getTestBinariesOfModule(NbJavaModule module) {
        File testBuildDir = module.getProperties().getOutput().getTestBuildDir();
        try {
            URL url = Utilities.toURI(testBuildDir).toURL();
            return new URL[]{url};
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.INFO, "Cannot convert to URL: " + testBuildDir, ex);
        }

        return NO_ROOTS;
    }

    private static URL[] tryGetRoots(
            NbJavaModule module, FileObject root) {
        SourceType sourceType = getSourceRootType(module, root);
        switch (sourceType) {
            case NORMAL:
                return getBinariesOfModule(module);
            case TEST:
                return getTestBinariesOfModule(module);
            default:
                return NO_ROOTS;

        }
    }

    @Override
    public void onModelChange() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                changes.fireChange();
            }
        });
    }

    @Override
    public BinaryForSourceQuery.Result findBinaryRoots(URL sourceRoot) {
        File sourceRootFile = FileUtil.archiveOrDirForURL(sourceRoot);
        if (sourceRootFile == null) {
            return null;
        }
        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRootFile);
        if (sourceRootObj == null) {
            return null;
        }

        if (!javaExt.isOwnerProject(sourceRootObj)) {
            return null;
        }

        NbJavaModule mainModule = javaExt.getCurrentModel().getMainModule();
        SourceType rootType = getSourceRootType(mainModule, sourceRootObj);
        if (rootType == SourceType.UNKNOWN) {
            return null;
        }

        BinaryForSourceQuery.Result result = cache.get(sourceRootObj);
        if (result != null) {
            return result;
        }

        result = new BinaryForSourceQuery.Result() {
            @Override
            public URL[] getRoots() {
                NbJavaModule mainModule = javaExt.getCurrentModel().getMainModule();

                return tryGetRoots(mainModule, sourceRootObj);
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

    private static final class EventSource implements BinaryForSourceQuery.Result {
        private volatile ChangeSupport changes;

        public void init(ChangeSupport changes) {
            assert changes != null;
            this.changes = changes;
        }

        @Override
        public URL[] getRoots() {
            return NO_ROOTS;
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            changes.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            changes.removeChangeListener(l);
        }
    }
}
