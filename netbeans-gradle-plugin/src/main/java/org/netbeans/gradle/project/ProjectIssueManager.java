package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.properties.SwingPropertyChangeForwarder;
import org.netbeans.spi.project.ui.ProjectProblemsProvider;

public final class ProjectIssueManager {
    private final Lock mainLock;
    private final RefList<ProjectIssue> issues;
    private final ChangeListenerManager changeListeners;

    private final IssueProperty issuesProperty;
    private final ProjectProblemsProviderImpl projectProblemsProvider;

    public ProjectIssueManager() {
        this.mainLock = new ReentrantLock();
        this.issues = new RefLinkedList<>();
        this.changeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.issuesProperty = new IssueProperty();
        this.projectProblemsProvider = new ProjectProblemsProviderImpl(issuesProperty);
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

    public PropertySource<Collection<ProjectIssue>> issues() {
        return issuesProperty;
    }

    public boolean hasIssues() {
        mainLock.lock();
        try {
            return !issues.isEmpty();
        } finally {
            mainLock.unlock();
        }
    }

    public Collection<ProjectIssue> getIssues() {
        mainLock.lock();
        try {
            return new ArrayList<>(issues);
        } finally {
            mainLock.unlock();
        }
    }

    public ProjectIssueRef createIssueRef() {
        return new ProjectIssueRefImpl();
    }

    private class ProjectIssueRefImpl implements ProjectIssueRef {
        private RefList.ElementRef<ProjectIssue> infoRef;

        public ProjectIssueRefImpl() {
            this.infoRef = null;
        }

        @Override
        public void setInfo(ProjectIssue info) {
            ProjectIssue prevInfo;
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
                        infoRef = issues.addLastGetReference(info);
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

    private final class IssueProperty implements PropertySource<Collection<ProjectIssue>> {
        @Override
        public Collection<ProjectIssue> getValue() {
            return getIssues();
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return ProjectIssueManager.this.addChangeListener(listener);
        }
    }

    private static class ProjectProblemsProviderImpl implements ProjectProblemsProvider {
        private final PropertySource<Collection<ProjectIssue>> informations;
        private final SwingPropertyChangeForwarder properties;

        public ProjectProblemsProviderImpl(PropertySource<Collection<ProjectIssue>> informations) {
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
            Collection<ProjectIssue> projectInfos = informations.getValue();
            Collection<ProjectProblemsProvider.ProjectProblem> result = new ArrayList<>(projectInfos.size());

            for (ProjectIssue info: projectInfos) {
                addProblems(info, result);
            }
            return result;
        }

        private static ProjectProblemsProvider.ProjectProblem asProblem(ProjectIssue.Entry entry) {
            switch (entry.getKind()) {
                case WARNING:
                    return ProjectProblemsProvider.ProjectProblem.createWarning(
                            entry.getSummary(),
                            entry.getDetails());
                case ERROR:
                    return ProjectProblemsProvider.ProjectProblem.createError(
                            entry.getSummary(),
                            entry.getDetails());
                default:
                    return null;
            }
        }

        private static void addProblems(
                ProjectIssue info,
                Collection<? super ProjectProblemsProvider.ProjectProblem> result) {
            for (ProjectIssue.Entry entry: info.getEntries()) {
                ProjectProblemsProvider.ProjectProblem problem = asProblem(entry);
                if (problem != null) {
                    result.add(problem);
                }
            }
        }
    }
}
