package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.event.ListenerRef;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;

public final class ProjectInfoManager {
    private final Lock mainLock;
    private final RefList<ProjectInfo> informations;
    private final ChangeListenerManager changeListeners;

    public ProjectInfoManager() {
        this.mainLock = new ReentrantLock();
        this.informations = new RefLinkedList<>();
        this.changeListeners = GenericChangeListenerManager.getSwingNotifier();
    }

    public ListenerRef addChangeListener(Runnable listener) {
        return changeListeners.registerListener(listener);
    }

    private void fireChange() {
        changeListeners.fireEventually();
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

            if (Objects.equals(prevInfo, info)) {
                fireChange();
            }
        }
    }
}
