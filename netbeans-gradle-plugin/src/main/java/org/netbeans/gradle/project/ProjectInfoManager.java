package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.DoneFuture;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.properties.SwingPropertyChangeForwarder;
import org.netbeans.spi.project.ui.ProjectProblemResolver;
import org.netbeans.spi.project.ui.ProjectProblemsProvider;

public final class ProjectInfoManager {
    private final Lock mainLock;
    private final RefList<ProjectInfo> informations;
    private final ChangeListenerManager changeListeners;

    private final InformationsProperty informationsProperty;
    private final ProjectProblemsProviderImpl projectProblemsProvider;

    public ProjectInfoManager() {
        this.mainLock = new ReentrantLock();
        this.informations = new RefLinkedList<>();
        this.changeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.informationsProperty = new InformationsProperty();
        this.projectProblemsProvider = new ProjectProblemsProviderImpl(informationsProperty);
    }

    public ProjectProblemsProvider asProblemProvider() {
        return projectProblemsProvider;
    }

    public ListenerRef addChangeListener(Runnable listener) {
        return changeListeners.registerListener(listener);
    }

    private void fireChange() {
        changeListeners.fireEventually();
    }

    public PropertySource<Collection<ProjectInfo>> informations() {
        return informationsProperty;
    }

    public boolean hasProblems() {
        mainLock.lock();
        try {
            return !informations.isEmpty();
        } finally {
            mainLock.unlock();
        }
    }

    public Collection<ProjectInfo> getInformations() {
        mainLock.lock();
        try {
            return new ArrayList<>(informations);
        } finally {
            mainLock.unlock();
        }
    }

    public ProjectInfoRef createInfoRef() {
        return new ProjectInfoRefImpl();
    }

    private class ProjectInfoRefImpl implements ProjectInfoRef {
        private RefList.ElementRef<ProjectInfo> infoRef;

        public ProjectInfoRefImpl() {
            this.infoRef = null;
        }

        @Override
        public void setInfo(ProjectInfo info) {
            ProjectInfo prevInfo;
            mainLock.lock();
            try {
                prevInfo = infoRef != null ? infoRef.getElement() : null;

                if (info == null) {
                    if (infoRef != null) {
                        infoRef.remove();
                        infoRef = null;
                    }
                }
                else {
                    if (infoRef != null) {
                        infoRef.setElement(info);
                    }
                    else {
                        infoRef = informations.addLastGetReference(info);
                    }
                }
            } finally {
                mainLock.unlock();
            }

            if (!Objects.equals(prevInfo, info)) {
                fireChange();
            }
        }
    }

    private final class InformationsProperty implements PropertySource<Collection<ProjectInfo>> {
        @Override
        public Collection<ProjectInfo> getValue() {
            return getInformations();
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return ProjectInfoManager.this.addChangeListener(listener);
        }
    }

    private static enum UnresolvableError implements ProjectProblemResolver {
        INSTANCE;

        private static final ProjectProblemsProvider.Result UNRESOLVED_RESULT
                = ProjectProblemsProvider.Result.create(ProjectProblemsProvider.Status.UNRESOLVED);

        @Override
        public Future<ProjectProblemsProvider.Result> resolve() {
            return new DoneFuture<>(UNRESOLVED_RESULT);
        }
    }

    private static class ProjectProblemsProviderImpl implements ProjectProblemsProvider {
        private final PropertySource<Collection<ProjectInfo>> informations;
        private final SwingPropertyChangeForwarder properties;

        public ProjectProblemsProviderImpl(PropertySource<Collection<ProjectInfo>> informations) {
            this.informations = informations;

            SwingPropertyChangeForwarder.Builder propertiesBuilder = new SwingPropertyChangeForwarder.Builder();
            propertiesBuilder.addProperty(ProjectProblemsProvider.PROP_PROBLEMS, informations);
            this.properties = propertiesBuilder.create();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            properties.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            properties.removePropertyChangeListener(listener);
        }

        @Override
        public Collection<? extends ProjectProblemsProvider.ProjectProblem> getProblems() {
            Collection<ProjectInfo> projectInfos = informations.getValue();
            Collection<ProjectProblemsProvider.ProjectProblem> result = new ArrayList<>(projectInfos.size());

            for (ProjectInfo info: projectInfos) {
                addProblems(info, result);
            }
            return result;
        }

        private static ProjectProblemsProvider.ProjectProblem asProblem(ProjectInfo.Entry entry) {
            switch (entry.getKind()) {
                case WARNING:
                    return ProjectProblemsProvider.ProjectProblem.createWarning(
                            "Warning",
                            entry.getInfo(),
                            UnresolvableError.INSTANCE);
                case ERROR:
                    return ProjectProblemsProvider.ProjectProblem.createError(
                            "Error",
                            entry.getInfo(),
                            UnresolvableError.INSTANCE);
                default:
                    return null;
            }
        }

        private static void addProblems(
                ProjectInfo info,
                Collection<? super ProjectProblemsProvider.ProjectProblem> result) {
            for (ProjectInfo.Entry entry: info.getEntries()) {
                ProjectProblemsProvider.ProjectProblem problem = asProblem(entry);
                if (problem != null) {
                    result.add(problem);
                }
            }
        }
    }
}
