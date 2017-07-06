package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.PropertySource;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.query.GradleHomeClassPathProvider;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;

public final class GradleHomeRegistry {
    private static final UpdateTaskExecutor GRADLE_HOME_UPDATER
            = NbTaskExecutors.newDefaultUpdateExecutor();

    private static final AtomicReference<GradleHomePaths> GRADLE_HOME_BINARIES;
    private static final AtomicBoolean REGISTERED_GLOBAL_PATH;
    private static final AtomicBoolean USING_GLOBAL_PATHS;
    private static final PropertyChangeSupport CHANGES;

    static {
        EventSource eventSource = new EventSource();
        CHANGES = new PropertyChangeSupport(eventSource);
        eventSource.init(CHANGES);

        USING_GLOBAL_PATHS = new AtomicBoolean(false);
        REGISTERED_GLOBAL_PATH = new AtomicBoolean(false);
        GRADLE_HOME_BINARIES = new AtomicReference<>(new GradleHomePaths());
    }

    private static void doRegisterGlobalClassPath() {
        ClassPath classPath = ClassPathFactory.createClassPath(new ClassPathImplementation() {
            @Override
            public List<PathResourceImplementation> getResources() {
                return GRADLE_HOME_BINARIES.get().getPaths();
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener listener) {
                CHANGES.addPropertyChangeListener(listener);
            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener listener) {
                CHANGES.removePropertyChangeListener(listener);
            }
        });

        GlobalPathRegistry.getDefault().register(ClassPath.COMPILE, new ClassPath[]{classPath});
    }

    private static PropertySource<GradleLocationDef> gradleLocation() {
        return CommonGlobalSettings.getDefault().gradleLocation().getActiveSource();
    }

    public static void requireGradlePaths() {
        if (USING_GLOBAL_PATHS.compareAndSet(false, true)) {
            gradleLocation().addChangeListener(GradleHomeRegistry::updateGradleHome);
            updateGradleHome();
        }
    }

    private static void updateGradleHome() {
        FileObject gradleHome = CommonGlobalSettings.getDefault().tryGetGradleInstallation();
        if (gradleHome != null) {
            setGradleHome(gradleHome);
        }
    }

    private static void setGradleHome(final FileObject gradleHome) {
        Objects.requireNonNull(gradleHome, "gradleHome");

        GRADLE_HOME_UPDATER.execute(() -> {
            URL[] urls = GradleHomeClassPathProvider.getAllGradleLibs(gradleHome);
            if (urls.length == 0) {
                // Keep the previous classpaths if there are non found.
                return;
            }

            if (gradleHome.equals(GRADLE_HOME_BINARIES.get().getHomePath())) {
                return;
            }

            GRADLE_HOME_BINARIES.set(new GradleHomePaths(gradleHome, urls));

            SwingUtilities.invokeLater(() -> {
                CHANGES.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null);
            });

            if (REGISTERED_GLOBAL_PATH.compareAndSet(false, true)) {
                doRegisterGlobalClassPath();
            }
        });
    }

    private static class GradleHomePaths {
        private final FileObject homePath;
        private final List<PathResourceImplementation> paths;

        public GradleHomePaths() {
            this(null, new URL[0]);
        }

        public GradleHomePaths(FileObject homePath, URL[] urls) {
            ExceptionHelper.checkNotNullElements(urls, "urls");

            this.homePath = homePath;

            List<PathResourceImplementation> resources = new ArrayList<>(urls.length);
            for (URL url: urls) {
                resources.add(ClassPathSupport.createResource(url));
            }
            this.paths = Collections.unmodifiableList(resources);
        }

        public FileObject getHomePath() {
            return homePath;
        }

        public List<PathResourceImplementation> getPaths() {
            return paths;
        }
    }

    private static final class EventSource implements ClassPathImplementation {
        private volatile PropertyChangeSupport changes;

        public void init(PropertyChangeSupport changes) {
            assert changes != null;
            this.changes = changes;
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            return Collections.emptyList();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changes.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changes.removePropertyChangeListener(listener);
        }
    }
}
