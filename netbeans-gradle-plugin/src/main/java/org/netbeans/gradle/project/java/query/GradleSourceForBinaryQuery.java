package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.query.AbstractSourceForBinaryQuery;
import org.netbeans.gradle.project.util.LazyChangeSupport;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSourceForBinaryQuery
extends
        AbstractSourceForBinaryQuery
implements
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

    private final Supplier<? extends NbJavaModule> moduleProvider;
    private final LazyChangeSupport changes;

    public GradleSourceForBinaryQuery(final JavaExtension javaExt) {
        this(() -> javaExt.getCurrentModel().getMainModule());

        Objects.requireNonNull(javaExt, "javaExt");
    }

    public GradleSourceForBinaryQuery(Supplier<? extends NbJavaModule> moduleProvider) {
        this.moduleProvider = Objects.requireNonNull(moduleProvider, "moduleProvider");
        this.changes = LazyChangeSupport.createSwing(new EventSource());
    }

    private static List<File> tryGetSourceRoots(NbJavaModule module, File binaryRoot) {
        List<JavaSourceSet> jarSources = module.getSourceSetsForJarOutput(binaryRoot);
        if (!jarSources.isEmpty()) {
            List<File> result = new ArrayList<>();
            for (JavaSourceSet sourceSet: module.getSources()) {
                for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                    result.addAll(sourceGroup.getSourceRoots());
                }
            }
            return result;
        }

        for (JavaSourceSet sourceSet: module.getSources()) {
            JavaOutputDirs outputDirs = sourceSet.getOutputDirs();
            if (outputDirs.getClassesDirs().contains(binaryRoot)) {
                List<File> result = new ArrayList<>();

                for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                    result.addAll(sourceGroup.getSourceRoots());
                }

                return result;
            }
        }
        return null;
    }

    private static FileObject[] getSourceRoots(
            NbJavaModule module, File binaryRoot) {

        List<File> srcRoots = tryGetSourceRoots(module, binaryRoot);
        if (srcRoots == null) {
            return NO_ROOTS;
        }

        List<FileObject> result = new ArrayList<>(srcRoots.size());

        for (File srcRoot: srcRoots) {
            FileObject rootObj = FileUtil.toFileObject(srcRoot);
            if (rootObj != null) {
                result.add(rootObj);
            }
        }

        return result.toArray(new FileObject[result.size()]);
    }

    @Override
    public void onModelChange() {
        changes.fireChange();
    }

    @Override
    protected File normalizeBinaryPath(File binaryRoot) {
        NbJavaModule mainModule = moduleProvider.get();

        // Is Jar output?
        if (!mainModule.getSourceSetsForJarOutput(binaryRoot).isEmpty()) {
            return binaryRoot;
        }

        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            JavaOutputDirs outputDirs = sourceSet.getOutputDirs();
            for (File classesDir: outputDirs.getClassesDirs()) {
            if (NbFileUtils.isParentOrSame(classesDir, binaryRoot)) {
                return classesDir;
            }
        }
        }
        return null;
    }

    @Override
    protected Result tryFindSourceRoot(final File binaryRoot) {
        NbJavaModule mainModule = moduleProvider.get();
        if (tryGetSourceRoots(mainModule, binaryRoot) == null) {
            return null;
        }

        return new SourceForBinaryQueryImplementation2.Result() {
            @Override
            public boolean preferSources() {
                return getRoots().length > 0;
            }

            @Override
            public FileObject[] getRoots() {
                NbJavaModule mainModule = moduleProvider.get();

                return getSourceRoots(mainModule, binaryRoot);
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
    }

    private static final class EventSource
    implements
            SourceForBinaryQueryImplementation2.Result,
            LazyChangeSupport.Source {

        private volatile LazyChangeSupport changes;

        @Override
        public void init(LazyChangeSupport changes) {
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
}
