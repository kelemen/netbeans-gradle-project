package org.netbeans.gradle.project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.util.CloseableAction;
import org.netbeans.gradle.project.util.NbFileUtils;

public final class RootProjectRegistry {
    private static final RootProjectRegistry DEFAULT = new RootProjectRegistry();

    private final Lock mainLock;
    private final Map<RootProjectKey, RegisteredProjects> rootProjects;

    public RootProjectRegistry() {
        this.mainLock = new ReentrantLock();
        this.rootProjects = new HashMap<>();
    }

    public static RootProjectRegistry getDefault() {
        return DEFAULT;
    }

    private static boolean isExplicitRootProject(NbGradleModel input) {
        if (!input.isRootProject()) {
            return false;
        }

        File settingsFile = input.getSettingsFile();
        if (settingsFile == null) {
            return false;
        }

        return Files.isRegularFile(settingsFile.toPath());
    }

    private CloseableAction.Ref registerAndUpdateProjects(NbGradleModel input) {
        if (!isExplicitRootProject(input)) {
            return CloseableAction.CLOSED_REF;
        }

        CloseableAction.Ref result = registerRootProjectModel(input);
        try {
            updateProjects(input);
        } catch (Throwable ex) {
            try {
                result.close();
            } catch (Throwable subEx) {
                ex.addSuppressed(subEx);
            }
            throw ex;
        }
        return result;
    }

    private static void updateProjects(NbGradleModel model) {
        updateProjects(LoadedProjectManager.getDefault(), model.getSettingsFile(), model.getMainProject());
    }

    private static void updateProjects(
            LoadedProjectManager loadedProjects,
            File settingsFile,
            NbGradleProjectTree projectTree) {

        for (NbGradleProjectTree child: projectTree.getChildren()) {
            NbGradleProject childProject = loadedProjects.tryGetLoadedProject(child.getProjectDir());
            if (childProject != null) {
                childProject.updateSettingsFile();
            }

            updateProjects(loadedProjects, settingsFile, child);
        }
    }

    private CloseableAction registerAsCloseableAction(final NbGradleModel input) {
        return new CloseableAction() {
            @Override
            public CloseableAction.Ref open() {
                return registerAndUpdateProjects(input);
            }
        };
    }

    public PropertySource<CloseableAction> forProject(NbGradleProject project) {
        return PropertyFactory.convert(project.currentModel(), new ValueConverter<NbGradleModel, CloseableAction>() {
            @Override
            public CloseableAction convert(NbGradleModel input) {
                return registerAsCloseableAction(input);
            }
        });
    }

    public CloseableAction.Ref registerRootProjectModel(NbGradleModel model) {
        final RootProjectKey key = new RootProjectKey(model);
        RegisteredProjects registeredProjects = new RegisteredProjects(model);
        final Object regId = registeredProjects.id;

        mainLock.lock();
        try {
            rootProjects.put(key, registeredProjects);
        } finally {
            mainLock.unlock();
        }

        return new CloseableAction.Ref() {
            @Override
            public void close() {
                mainLock.lock();
                try {
                    RegisteredProjects value = rootProjects.get(key);
                    if (value != null && value.id == regId) {
                        rootProjects.remove(key);
                    }
                } finally {
                    mainLock.unlock();
                }
            }
        };
    }

    public Path tryGetSettingsFile(File projectDir) {
        mainLock.lock();
        try {
            for (Map.Entry<RootProjectKey, RegisteredProjects> entry: rootProjects.entrySet()) {
                if (entry.getValue().subprojects.contains(projectDir)) {
                    return entry.getKey().settingsFile;
                }
            }
            return null;
        } finally {
            mainLock.unlock();
        }
    }

    private static Set<File> collectProjectDirs(NbGradleProjectTree root) {
        Set<File> result = new HashSet<>();
        collectProjectDirs(root, result);
        return result;
    }

    private static void collectProjectDirs(NbGradleProjectTree root, Set<? super File> result) {
        for (NbGradleProjectTree child: root.getChildren()) {
            result.add(child.getProjectDir());
            collectProjectDirs(child, result);
        }
    }

    private static final class RegisteredProjects {
        private final Object id;
        private final Set<File> subprojects;

        public RegisteredProjects(NbGradleModel model) {
            ExceptionHelper.checkNotNullArgument(model, "model");

            this.id = new Object();

            NbGradleProjectTree root = model.getProjectDef().getRootProject();
            this.subprojects = Collections.unmodifiableSet(collectProjectDirs(root));
        }
    }

    private static final class RootProjectKey {
        private final Path settingsFile;
        private final Path projectDir;

        public RootProjectKey(NbGradleModel model) {
            this(model.getSettingsFile(), model.getProjectDir());
        }

        public RootProjectKey(File settingsFile, File projectDir) {
            this(NbFileUtils.asPath(settingsFile), NbFileUtils.asPath(projectDir));
        }

        public RootProjectKey(Path settingsFile, Path projectDir) {
            ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");

            this.settingsFile = settingsFile;
            this.projectDir = projectDir;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.settingsFile);
            hash = 29 * hash + Objects.hashCode(this.projectDir);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final RootProjectKey other = (RootProjectKey)obj;
            return Objects.equals(this.settingsFile, other.settingsFile)
                    && Objects.equals(this.projectDir, other.projectDir);
        }
    }
}
