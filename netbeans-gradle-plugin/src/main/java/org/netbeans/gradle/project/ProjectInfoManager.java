package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;

public final class ProjectInfoManager {
    private final Lock mainLock;
    private final Map<InfoKey, ProjectInfo> informations;
    private final ListenerManager<Runnable> changeListeners;
    private final AtomicBoolean hasUnprocessedChangeEvent;

    public ProjectInfoManager() {
        this.mainLock = new ReentrantLock();
        this.informations = new HashMap<>();
        this.changeListeners = new CopyOnTriggerListenerManager<>();
        this.hasUnprocessedChangeEvent = new AtomicBoolean(false);
    }

    public ListenerRef addChangeListener(Runnable listener) {
        return changeListeners.registerListener(listener);
    }

    private void fireChange() {
        if (hasUnprocessedChangeEvent.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    hasUnprocessedChangeEvent.set(false);
                    EventListeners.dispatchRunnable(changeListeners);
                }
            });
        }
    }

    public Collection<ProjectInfo> getInformations() {
        mainLock.lock();
        try {
            return new ArrayList<>(informations.values());
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
