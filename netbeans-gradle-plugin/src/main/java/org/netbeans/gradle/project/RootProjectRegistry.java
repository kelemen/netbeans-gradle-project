package org.netbeans.gradle.project;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.util.CloseableAction;
import org.netbeans.gradle.project.util.Closeables;
import org.netbeans.gradle.project.util.NbFileUtils;

public final class RootProjectRegistry {
    private final Lock mainLock;
    private final Map<RootProjectKey, RegisteredProjects> rootProjects;

    public RootProjectRegistry() {
        this.mainLock = new ReentrantLock();
        this.rootProjects = new HashMap<>();
    }

    private static boolean isExplicitRootProject(NbGradleModel input) {
        if (!input.isRootProject()) {
            return false;
        }

        Path settingsFile = input.getSettingsFile();
        return settingsFile != null ? Files.isRegularFile(settingsFile) : false;
    }

    private CloseableAction.Ref registerAndUpdateProjects(NbGradleModel input) {
        if (!isExplicitRootProject(input)) {
            return CloseableAction.CLOSED_REF;
        }

        return registerRootProjectModel(input);
    }

    private CloseableAction registerAsCloseableAction(NbGradleModel input) {
        return () -> registerAndUpdateProjects(input);
    }

    public PropertySource<CloseableAction> forProject(PropertySource<? extends NbGradleModel> currentModel) {
        return PropertyFactory.convert(currentModel, this::registerAsCloseableAction);
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

        final List<Closeable> safeRefs = new ArrayList<>();
        CloseableAction.Ref result = () -> {
            mainLock.lock();
            try {
                RegisteredProjects value = rootProjects.get(key);
                if (value != null && value.id == regId) {
                    rootProjects.remove(key);
                }

                Closeables.closeAll(safeRefs);
            } finally {
                mainLock.unlock();
            }
        };

        try {
            safeToOpenChildren(model.getProjectDef().getRootProject(), safeRefs);
        } catch (Throwable ex) {
            result.close();
            throw ex;
        }

        return result;
    }

    private static void safeToOpenChildren(NbGradleProjectTree root, Collection<? super Closeable> safeRefs) {
        for (NbGradleProjectTree child: root.getChildren()) {
            safeRefs.add(NbGradleProjectFactory.safeToOpen(child.getProjectDir()));
            safeToOpenChildren(child, safeRefs);
        }
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
            Objects.requireNonNull(model, "model");

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

        public RootProjectKey(Path settingsFile, File projectDir) {
            this(settingsFile, NbFileUtils.asPath(projectDir));
        }

        public RootProjectKey(Path settingsFile, Path projectDir) {
            this.settingsFile = settingsFile;
            this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
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
