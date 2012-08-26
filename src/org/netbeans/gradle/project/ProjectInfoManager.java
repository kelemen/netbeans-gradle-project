package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

public final class ProjectInfoManager {
    private final Lock mainLock;
    private final Map<InfoKey, ProjectInfo> informations;
    private final ChangeSupport changes;
    private final AtomicBoolean hasUnprocessedChangeEvent;

    public ProjectInfoManager() {
        this.mainLock = new ReentrantLock();
        this.informations = new HashMap<InfoKey, ProjectInfo>();
        this.changes = new ChangeSupport(this);
        this.hasUnprocessedChangeEvent = new AtomicBoolean(false);
    }

    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    private void fireChange() {
        if (hasUnprocessedChangeEvent.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    hasUnprocessedChangeEvent.set(false);
                    changes.fireChange();
                }
            });
        }
    }

    public Collection<ProjectInfo> getInformations() {
        mainLock.lock();
        try {
            return new ArrayList<ProjectInfo>(informations.values());
        } finally {
            mainLock.unlock();
        }
    }

    public ProjectInfoRef createInfoRef() {
        return new ProjectInfoRefImpl();
    }

    private class ProjectInfoRefImpl implements ProjectInfoRef {
        private final InfoKey key;

        public ProjectInfoRefImpl() {
            this.key = new InfoKey();
        }

        @Override
        public void setInfo(ProjectInfo info) {
            ProjectInfo prevInfo;
            mainLock.lock();
            try {
                if (info == null) {
                    prevInfo = informations.remove(key);
                }
                else {
                    prevInfo = informations.put(key, info);
                }
            } finally {
                mainLock.unlock();
            }

            if (prevInfo != info) {
                fireChange();
            }
        }
    }

    // Only defined for type safety, could be Object otherwise.
    private static class InfoKey {
    }
}
