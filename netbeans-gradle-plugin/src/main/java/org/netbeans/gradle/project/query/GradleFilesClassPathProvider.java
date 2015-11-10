package org.netbeans.gradle.project.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = ClassPathProvider.class)})
public final class GradleFilesClassPathProvider implements ClassPathProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleFilesClassPathProvider.class.getName());

    private volatile boolean initialized;
    private final ReentrantLock initLock;
    private final ConcurrentMap<ClassPathType, List<PathResourceImplementation>> classpathResources;
    private final Map<ClassPathType, ClassPath> classpaths;
    private final UpdateTaskExecutor classpathUpdateExecutor;

    private final PropertyChangeSupport changes;

    @SuppressWarnings("MapReplaceableByEnumMap") // no, it's not.
    public GradleFilesClassPathProvider() {
        this.initLock = new ReentrantLock();
        this.initialized = false;
        this.classpaths = new EnumMap<>(ClassPathType.class);
        this.classpathResources = new ConcurrentHashMap<>();
        this.classpathUpdateExecutor = NbTaskExecutors.newDefaultUpdateExecutor();

        EventSource eventSource = new EventSource();
        this.changes = new PropertyChangeSupport(eventSource);
        eventSource.init(this.changes);
    }

    public static boolean isGradleFile(FileObject file) {
        // case-insensitive check, so that there is no surprise on Windows.
        return SettingsFiles.DEFAULT_GRADLE_EXTENSION_WITHOUT_DOT.equalsIgnoreCase(file.getExt());
    }

    // These PropertyChangeListener methods are declared because
    // for some reason, NetBeans want to use them through reflection.
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    private ClassPath createClassPath(ClassPathType classPathType) {
        return ClassPathFactory.createClassPath(new GradleClassPaths(classPathType));
    }

    private static URL[] getGradleBinaries() {
        FileObject gradleHome = GlobalGradleSettings.getDefault().getGradleLocation();
        if (gradleHome == null) {
            return new URL[0];
        }

        return GradleHomeClassPathProvider.getGradleLibs(gradleHome, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowerCaseName = name.toLowerCase(Locale.US);
                return !lowerCaseName.startsWith("groovy-") && lowerCaseName.endsWith(".jar");
            }
        });
    }

    private void updateClassPathResources() {
        URL[] jars = getGradleBinaries();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE,
                    "Updating the .gradle file classpaths to: {0}",
                    Arrays.toString(jars));
        }

        List<PathResourceImplementation> jarResources = new ArrayList<>(jars.length);
        for (URL jar: jars) {
            jarResources.add(ClassPathSupport.createResource(jar));
        }

        classpathResources.put(ClassPathType.COMPILE, jarResources);
        classpathResources.put(ClassPathType.RUNTIME, jarResources);

        JavaPlatform platform = GlobalGradleSettings.getDefault().gradleJdk().getValue();
        if (platform != null) {
            List<PathResourceImplementation> platformResources = new LinkedList<>();
            for (ClassPath.Entry entry: platform.getBootstrapLibraries().entries()) {
                platformResources.add(ClassPathSupport.createResource(entry.getURL()));
            }
            classpathResources.put(ClassPathType.BOOT, platformResources);
        }
    }

    private void setupClassPaths() {
        updateClassPathResources();

        classpaths.put(ClassPathType.BOOT, createClassPath(ClassPathType.BOOT));
        classpaths.put(ClassPathType.COMPILE, createClassPath(ClassPathType.COMPILE));
        classpaths.put(ClassPathType.RUNTIME, createClassPath(ClassPathType.RUNTIME));
    }

    private void init() {
        if (initialized) {
            return;
        }

        initLock.lock();
        try {
            if (!initialized) {
                unsafeInit();
            }
        } finally {
            initialized = true;
            initLock.unlock();
        }
    }

    private void scheduleUpdateClassPath() {
        classpathUpdateExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateClassPathResources();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        changes.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null);
                    }
                });
            }
        });
    }

    private void unsafeInit() {
        assert initLock.isHeldByCurrentThread();

        Runnable changeListener = new Runnable() {
            @Override
            public void run() {
                scheduleUpdateClassPath();
            }
        };

        GlobalGradleSettings.getDefault().gradleLocation().addChangeListener(changeListener);
        GlobalGradleSettings.getDefault().gradleJdk().addChangeListener(changeListener);

        setupClassPaths();
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        // case-insensitive check, so that there is no surprise on Windows.
        if (!isGradleFile(file)) {
            return null;
        }

        init();

        ClassPathType classPathType = getClassPathType(type);
        if (classPathType == null) {
            return null;
        }

        ClassPath classpath = classpaths.get(classPathType);
        if (classpath != null) {
            return classpath;
        }

        setupClassPaths();
        return classpaths.get(classPathType);
    }

    private static ClassPathType getClassPathType(String type) {
        if (type == null) {
            return null;
        }

        switch (type) {
            case ClassPath.SOURCE:
                return null;
            case ClassPath.BOOT:
                return ClassPathType.BOOT;
            case ClassPath.COMPILE:
                return ClassPathType.COMPILE;
            case ClassPath.EXECUTE:
                return ClassPathType.RUNTIME;
            case JavaClassPathConstants.PROCESSOR_PATH:
                return ClassPathType.COMPILE;
            default:
                return null;
        }
    }

    private class GradleClassPaths implements ClassPathImplementation {
        private final ClassPathType classPathType;

        public GradleClassPaths(ClassPathType classPathType) {
            assert classPathType != null;
            this.classPathType = classPathType;
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            List<PathResourceImplementation> result = classpathResources.get(classPathType);
            return result != null
                    ? result
                    : Collections.<PathResourceImplementation>emptyList();
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

    private enum ClassPathType {
        BOOT,
        COMPILE,
        RUNTIME
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
