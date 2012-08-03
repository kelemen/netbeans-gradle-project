package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;
import org.openide.util.NbBundle;

public final class GradleClassPathProvider implements ClassPathProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleClassPathProvider.class.getName());

    public static final String RELATIVE_OUTPUT_PATH = "build/classes/main/";

    private final NbGradleProject project;
    private final AtomicReference<ClassPath> compileClassPath;
    private final AtomicReference<ClassPath> bootClassPath;

    public GradleClassPathProvider(NbGradleProject project) {
        this.project = project;
        this.compileClassPath = new AtomicReference<ClassPath>(null);
        this.bootClassPath = new AtomicReference<ClassPath>(null);
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        if (ClassPath.SOURCE.equals(type)) {
            ClassPath result = compileClassPath.get();
            if (result == null) {
                compileClassPath.compareAndSet(null, ClassPathFactory.createClassPath(
                        new BackgroundClassPathLoader(new SourceClassPathFinder(project, false))));
                result = compileClassPath.get();
            }
            return result;
        }
        else if (ClassPath.BOOT.equals(type)) {
            ClassPath result = bootClassPath.get();
            if (result == null) {
                bootClassPath.compareAndSet(null, ClassPathFactory.createClassPath(
                        new BackgroundClassPathLoader(new BootClassPathFinder(project))));
                result = bootClassPath.get();
            }
            return result;
        }
        else {
            return ClassPath.EMPTY;
        }
    }

    private static interface ResourceFinder {
        public List<PathResourceImplementation> findResources();
    }

    private static abstract class ProjectResourceFinder implements ResourceFinder {
        protected final NbGradleProject project;

        public ProjectResourceFinder(NbGradleProject project) {
            this.project = project;
        }

        public abstract List<PathResourceImplementation> findResources(NbProjectModel projectModel);

        @Override
        public final List<PathResourceImplementation> findResources() {
            ProgressHandle progress = ProgressHandleFactory.createHandle(
                    NbBundle.getMessage(GradleClassPathProvider.class, "LBL_CheckingForClassPaths"));
            progress.start();
            try {
                NbProjectModel projectModel = project.loadProject();
                return findResources(projectModel);
            } finally {
                progress.finish();
            }
        }
    }

    private static class BootClassPathFinder extends ProjectResourceFinder {
        private static final String IDEA_JAVA_VERSION_PREFIX = "JDK_";

        public BootClassPathFinder(NbGradleProject project) {
            super(project);
        }

        private static JavaPlatform findMostAppropriate(String javaVersion) {
            String normalizedVersion = javaVersion.trim();
            if (normalizedVersion.startsWith(IDEA_JAVA_VERSION_PREFIX)) {
                normalizedVersion = normalizedVersion.substring(IDEA_JAVA_VERSION_PREFIX.length());
            }
            normalizedVersion = normalizedVersion.replace('_', '.');

            if (!normalizedVersion.contains(".")) {
                normalizedVersion = "1." + normalizedVersion;
            }
            int dotCount = 0;
            for (int i = 0; i < normalizedVersion.length(); i++) {
                if (normalizedVersion.charAt(i) == '.') {
                    dotCount++;
                }
            }
            if (dotCount % 2 != 0) {
                normalizedVersion += ".0";
            }

            SpecificationVersion parsedVersion;
            try {
                parsedVersion = new SpecificationVersion(normalizedVersion);
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid required java version: {0}", javaVersion);
                return JavaPlatform.getDefault();
            }

            // FIXME: We have to refresh the boot path if a new JavaPlatform is
            //  added. This is not trivial here because there is no place to
            //  unregister such listener.
            JavaPlatform best = null;
            for (JavaPlatform platform: JavaPlatformManager.getDefault().getInstalledPlatforms()) {
                if (best == null) {
                    best = platform;
                }
                else {
                    SpecificationVersion bestVersion = best.getSpecification().getVersion();
                    SpecificationVersion platformVersion = platform.getSpecification().getVersion();
                    // We want to replace in two cases:
                    //
                    // 1. If the best version we have found is at least the
                    //    requested version, then we will replace it with a
                    //    lower version (if the lower version still enough).
                    // 2. If the required version is higher than the best we
                    //    have found, then replace it if the current one has a
                    //    higher version number.

                    if (bestVersion.compareTo(parsedVersion) >= 0) {
                        if (platformVersion.compareTo(parsedVersion) >= 0) {
                            if (bestVersion.compareTo(platformVersion) > 0) {
                                best = platform;
                            }
                        }
                    }
                    else if (platformVersion.compareTo(bestVersion) > 0) {
                        best = platform;
                    }
                }
            }

            if (best != null && parsedVersion.compareTo(best.getSpecification().getVersion()) > 0) {
                LOGGER.log(Level.WARNING,
                        "The JDK with the highest version number is {0} but the required one is {1}.",
                        new Object[]{best.getSpecification().getVersion(), parsedVersion});
            }

            if (best != null) {
                LOGGER.log(Level.FINE, "Using JDK: {0}.", best.getSpecification().getVersion());
            }

            return best != null ? best : JavaPlatform.getDefault();
        }

        private static void addPlatform(JavaPlatform platform, List<PathResourceImplementation> paths) {
            if (platform == null) {
                LOGGER.log(Level.WARNING, "Could not find any java platform.");
                return;
            }

            for (ClassPath.Entry entry: platform.getBootstrapLibraries().entries()) {
                paths.add(ClassPathSupport.createResource(entry.getURL()));
            }
        }

        private void addModuleClassPaths(
                HierarchicalEclipseProject project,
                List<PathResourceImplementation> paths) {

            File outputDir = new File(project.getProjectDirectory(), RELATIVE_OUTPUT_PATH);
            paths.add(ClassPathSupport.createResource(FileUtil.urlForArchiveOrDir(outputDir)));
        }

        private void addExternalClassPaths(ExternalDependency dependency, List<PathResourceImplementation> paths) {
            paths.add(ClassPathSupport.createResource(FileUtil.urlForArchiveOrDir(dependency.getFile())));
        }

        @Override
        public List<PathResourceImplementation> findResources(NbProjectModel projectModel) {
            List<PathResourceImplementation> result = new LinkedList<PathResourceImplementation>();

            // This is not actually good because this relies on the "idea"
            // plugin and not the "java".
//            String javaVersion = projectModel.getIdeaModel().getLanguageLevel().getLevel();
//            addPlatform(findMostAppropriate(javaVersion), result);
            addPlatform(JavaPlatform.getDefault(), result);

            IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
            EclipseProject mainProject = projectModel.getEclipseModel();
            addModuleClassPaths(mainProject, result);

            for (HierarchicalEclipseProject dependency: NbProjectModelUtils.getEclipseProjectDependencies(mainProject)) {
                addModuleClassPaths(dependency, result);
            }

            for (IdeaDependency dependency: NbProjectModelUtils.getIdeaDependencies(mainModule)) {
                if (dependency instanceof ExternalDependency) {
                    addExternalClassPaths((ExternalDependency)dependency, result);
                }
            }

            return result;
        }
    }

    private static class SourceClassPathFinder extends ProjectResourceFinder {
        private final boolean findTests;

        public SourceClassPathFinder(NbGradleProject project, boolean findTests) {
            super(project);
            this.findTests = findTests;
        }

        private void addSrcDir(IdeaSourceDirectory srcDir, List<PathResourceImplementation> paths) {
            if (!NbProjectModelUtils.isResourcePath(srcDir)) {
                paths.add(ClassPathSupport.createResource(FileUtil.urlForArchiveOrDir(srcDir.getDirectory())));
            }
        }

        private void addSrcPathsFromModule(IdeaModule module, List<PathResourceImplementation> paths) {
            for (IdeaContentRoot contentRoot: module.getContentRoots()) {
                for (IdeaSourceDirectory srcDir: contentRoot.getSourceDirectories()) {
                    addSrcDir(srcDir, paths);
                }

                if (findTests) {
                    for (IdeaSourceDirectory testDir: contentRoot.getSourceDirectories()) {
                        addSrcDir(testDir, paths);
                    }
                }
            }
        }

        @Override
        public List<PathResourceImplementation> findResources(NbProjectModel projectModel) {
            List<PathResourceImplementation> result = new LinkedList<PathResourceImplementation>();
            IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
            addSrcPathsFromModule(mainModule, result);

            for (IdeaModule projectDependency: NbProjectModelUtils.getIdeaProjectDependencies(mainModule)) {
                addSrcPathsFromModule(projectDependency, result);
            }

            LOGGER.log(Level.FINE, "Found source class paths: {0}", result);
            return result;
        }
    }

    private static class BackgroundClassPathLoader implements ClassPathImplementation {
        private final PropertyChangeSupport changes;
        private final ResourceFinder resourceFinder;
        private final AtomicBoolean refreshingResources;
        private volatile List<PathResourceImplementation> lastResources;

        public BackgroundClassPathLoader(ResourceFinder resourceFinder) {
            this.changes = new PropertyChangeSupport(this);
            this.resourceFinder = resourceFinder;
            this.refreshingResources = new AtomicBoolean(false);
            this.lastResources = null;
        }

        public void refreshResources() {
            if (refreshingResources.compareAndSet(false, true)) {
                NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        refreshingResources.set(false);
                        final List<PathResourceImplementation> newResources = resourceFinder.findResources();
                        lastResources = newResources;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                changes.firePropertyChange(PROP_RESOURCES, null, newResources);
                            }
                        });
                    }
                });
            }
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            List<PathResourceImplementation> currentResource = lastResources;
            if (currentResource == null) {
                refreshResources();
                currentResource = Collections.emptyList();
            }

            return Collections.unmodifiableList(currentResource);
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
