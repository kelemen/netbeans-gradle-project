package org.netbeans.gradle.project.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.script.DefaultScriptFileProvider;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.NbTaskExecutors;
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
    private final ScriptFileProvider scriptProvider;

    private final PropertyChangeSupport changes;

    public GradleFilesClassPathProvider() {
        this(new DefaultScriptFileProvider());
    }

    public GradleFilesClassPathProvider(ScriptFileProvider scriptProvider) {
        this.scriptProvider = Objects.requireNonNull(scriptProvider, "scriptProvider");
        this.initLock = new ReentrantLock();
        this.initialized = false;
        this.classpaths = new EnumMap<>(ClassPathType.class);
        this.classpathResources = new ConcurrentHashMap<>();
        this.classpathUpdateExecutor = NbTaskExecutors.newDefaultUpdateExecutor();

        EventSource eventSource = new EventSource();
        this.changes = new PropertyChangeSupport(eventSource);
        eventSource.init(this.changes);
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
        FileObject gradleHome = CommonGlobalSettings.getDefault().tryGetGradleInstallation();
        if (gradleHome == null) {
            return new URL[0];
        }

        return GradleHomeClassPathProvider.getGradleLibs(gradleHome, (File dir, String name) -> {
            String lowerCaseName = name.toLowerCase(Locale.US);
            return !lowerCaseName.startsWith("groovy-") && lowerCaseName.endsWith(".jar");
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

        ScriptPlatform platform = CommonGlobalSettings.getDefault().defaultJdk().getActiveValue();
        if (platform != null) {
            List<ClassPath.Entry> classpathEntries = platform.getJavaPlatform().getBootstrapLibraries().entries();
            List<PathResourceImplementation> platformResources = new ArrayList<>(classpathEntries.size());
            for (ClassPath.Entry entry: classpathEntries) {
                platformResources.add(ClassPathSupport.createResource(entry.getURL()));
            }
            classpathResources.put(ClassPathType.BOOT, Collections.unmodifiableList(platformResources));
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
        classpathUpdateExecutor.execute(() -> {
            updateClassPathResources();
            SwingUtilities.invokeLater(() -> {
                changes.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null);
            });
        });
    }

    private void unsafeInit() {
        assert initLock.isHeldByCurrentThread();

        Runnable changeListener = this::scheduleUpdateClassPath;

        CommonGlobalSettings defaultSettings = CommonGlobalSettings.getDefault();
        defaultSettings.gradleLocation().getActiveSource().addChangeListener(changeListener);
        defaultSettings.defaultJdk().getActiveSource().addChangeListener(changeListener);

        setupClassPaths();
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        if (!scriptProvider.isScriptFileName(file.getNameExt())) {
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
