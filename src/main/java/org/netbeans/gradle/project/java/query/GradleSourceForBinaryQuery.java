package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbOutput;
import org.netbeans.gradle.project.java.model.NbSourceType;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleSourceForBinaryQuery
implements
        SourceForBinaryQueryImplementation2,
        JavaModelChangeListener {
    // First, I thought this is an important query for the debugger but this
    // query is not really used by the debugger.
    //
    // Regardless, this query is important for the following reason:
    // NetBeans detects project dependencies the following way:
    //
    // First it queries the compile time dependencies, then it tries to look
    // up the sources of the copile time dependencies. If the sources should be
    // preferred over the binaries then it will not complain for the missing
    // binaries if the sources are available. Therefore the directory where the
    // compiled binaries of the project dependency will be stored are added to
    // the compile time classpath of the project. Adding sources of other
    // projects to the source path will confuse NetBeans.

    private static final FileObject[] NO_ROOTS = new FileObject[0];

    private final ConcurrentMap<File, SourceForBinaryQueryImplementation2.Result> cache;
    private final JavaExtension javaExt;
    private final ChangeSupport changes;

    public GradleSourceForBinaryQuery(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
        this.cache = new ConcurrentHashMap<File, SourceForBinaryQueryImplementation2.Result>();

        EventSource eventSource = new EventSource();
        this.changes = new ChangeSupport(eventSource);
        eventSource.init(this.changes);
    }

    private static BinaryType getBinaryRootType(
            NbJavaModule module, File root) {
        NbOutput output = module.getProperties().getOutput();

        if (GradleFileUtils.isParentOrSame(output.getBuildDir(), root)) {
            return BinaryType.NORMAL;
        }
        if (GradleFileUtils.isParentOrSame(output.getTestBuildDir(), root)) {
            return BinaryType.TEST;
        }
        return BinaryType.UNKNOWN;
    }

    private static FileObject[] getSourcesOfModule(NbJavaModule module) {
        List<FileObject> result = module.getSources(NbSourceType.SOURCE).getFileObjects();
        return result.toArray(NO_ROOTS);
    }

    private static FileObject[] getTestSourcesOfModule(NbJavaModule module) {
        List<FileObject> result = module.getSources(NbSourceType.TEST_SOURCE).getFileObjects();
        return result.toArray(NO_ROOTS);
    }

    private static FileObject[] tryGetRoots(
            NbJavaModule module, File root) {
        BinaryType binaryType = getBinaryRootType(module, root);
        switch (binaryType) {
            case NORMAL:
                return getSourcesOfModule(module);
            case TEST:
                return getTestSourcesOfModule(module);
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
    public SourceForBinaryQueryImplementation2.Result findSourceRoots2(URL binaryRoot) {
        final File binaryRootFile = FileUtil.archiveOrDirForURL(binaryRoot);
        if (binaryRootFile == null) {
            return null;
        }

        if (!javaExt.isOwnerProject(binaryRootFile)) {
            return null;
        }

        NbJavaModule mainModule = javaExt.getCurrentModel().getMainModule();
        BinaryType rootType = getBinaryRootType(mainModule, binaryRootFile);
        if (rootType == BinaryType.UNKNOWN) {
            return null;
        }

        SourceForBinaryQueryImplementation2.Result result = cache.get(binaryRootFile);
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
                NbJavaModel projectModel = javaExt.getCurrentModel();
                NbJavaModule mainModule = projectModel.getMainModule();

                return tryGetRoots(mainModule, binaryRootFile);
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
        SourceForBinaryQueryImplementation2.Result prevResult = cache.putIfAbsent(binaryRootFile, result);
        return prevResult != null ? prevResult : result;
    }

    @Override
    public SourceForBinaryQuery.Result findSourceRoots(URL binaryRoot) {
        return findSourceRoots2(binaryRoot);
    }

    private static final class EventSource implements SourceForBinaryQueryImplementation2.Result {
        private volatile ChangeSupport changes;

        public void init(ChangeSupport changes) {
            assert changes != null;
            this.changes = changes;
        }

        @Override
        public boolean preferSources() {
            return true;
        }

        @Override
        public FileObject[] getRoots() {
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

    private enum BinaryType {
        NORMAL,
        TEST,
        UNKNOWN
    }
}
