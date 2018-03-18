package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.query.AbstractBinaryForSourceQuery;
import org.netbeans.gradle.project.util.LazyChangeSupport;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbSupplier;
import org.openide.util.Utilities;

public final class GradleBinaryForSourceQuery
extends
        AbstractBinaryForSourceQuery
implements
        JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleSourceForBinaryQuery.class.getName());

    private static final URL[] NO_ROOTS = new URL[0];

    private final NbSupplier<? extends NbJavaModule> moduleProvider;
    private final LazyChangeSupport changes;

    public GradleBinaryForSourceQuery(final JavaExtension javaExt) {
        this(new NbSupplier<NbJavaModule>() {
            @Override
            public NbJavaModule get() {
                return javaExt.getCurrentModel().getMainModule();
            }
        });

        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");
    }

    public GradleBinaryForSourceQuery(NbSupplier<? extends NbJavaModule> moduleProvider) {
        ExceptionHelper.checkNotNullArgument(moduleProvider, "moduleProvider");

        this.moduleProvider = moduleProvider;
        this.changes = LazyChangeSupport.createSwing(new EventSource());
    }

    private static Set<File> getOutputDirs(
            NbJavaModule module, File root) {

        for (JavaSourceSet sourceSet: module.getSources()) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                for (File sourceRoot: sourceGroup.getSourceRoots()) {
                    if (Objects.equals(sourceRoot, root)) {
                        return sourceSet.getOutputDirs().getClassesDirs();
                    }
                }
            }
        }
        return Collections.emptySet();
    }

    private static URL toUrl(File file) {
        try {

            return Utilities.toURI(file).toURL();
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.INFO, "Cannot convert to URL: " + file, ex);
            return null;
        }
    }

    private static URL[] getRootsAsURLs(
            NbJavaModule module, File root) {

        Set<File> outputDirs = getOutputDirs(module, root);
        if (outputDirs.isEmpty()) {
            return NO_ROOTS;
        }

        List<URL> result = new ArrayList<>(outputDirs.size());
        for (File outputDir: outputDirs) {
            URL url = toUrl(outputDir);
            if (url != null) {
                result.add(url);
            }
        }
        return result.toArray(new URL[result.size()]);
    }

    @Override
    public void onModelChange() {
        changes.fireChange();
    }

    @Override
    protected File normalizeSourcePath(File sourcePath) {
        NbJavaModule module = moduleProvider.get();
        for (JavaSourceSet sourceSet: module.getSources()) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                for (File sourceRoot: sourceGroup.getSourceRoots()) {
                    if (NbFileUtils.isParentOrSame(sourceRoot, sourcePath)) {
                        return sourceRoot;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected BinaryForSourceQuery.Result tryFindBinaryRoots(final File sourceRoot) {
        // If source path normalization succeeds, it is a source root we own.

        return new BinaryForSourceQuery.Result() {
            @Override
            public URL[] getRoots() {
                NbJavaModule mainModule = moduleProvider.get();
                return getRootsAsURLs(mainModule, sourceRoot);
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
            BinaryForSourceQuery.Result,
            LazyChangeSupport.Source {
        private volatile LazyChangeSupport changes;

        @Override
        public void init(LazyChangeSupport changes) {
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
