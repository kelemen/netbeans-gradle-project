
package org.netbeans.gradle.project.properties;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.api.event.ListenerRef;
import org.netbeans.gradle.project.api.query.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.query.ProjectPlatform;
import org.openide.util.ChangeSupport;

public final class ProjectQueryPlatformSource implements PropertySource<ProjectPlatform> {
    private final GradleProjectPlatformQuery query;
    private final String name;
    private final String version;
    private final boolean defaultValue;

    private final Lock changesLock;
    private final ChangeSupport changes;
    private ListenerRef subListenerRef;

    public ProjectQueryPlatformSource(
            GradleProjectPlatformQuery query,
            String name,
            String version,
            boolean defaultValue) {
        if (query == null) throw new NullPointerException("query");
        if (name == null) throw new NullPointerException("name");
        if (version == null) throw new NullPointerException("version");

        this.query = query;
        this.name = name;
        this.version = version;
        this.defaultValue = defaultValue;
        this.changesLock = new ReentrantLock();
        this.changes = new ChangeSupport(this);
        this.subListenerRef = null;
    }

    @Override
    public ProjectPlatform getValue() {
        return query.tryFindPlatformByName(name, version);
    }

    @Override
    public boolean isDefault() {
        return defaultValue;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        ListenerRef toRemove = null;
        InitLaterListenerRef toAdd = null;

        changesLock.lock();
        try {
            if (!changes.hasListeners()) {
                toRemove = subListenerRef;
                toAdd = new InitLaterListenerRef();
                subListenerRef = toAdd;
            }
            changes.addChangeListener(listener);
        } finally {
            changesLock.unlock();
        }

        if (toRemove != null) {
            toRemove.unregister();
        }
        if (toAdd != null) {
            toAdd.init(query.addPlatformChangeListener(new Runnable() {
                @Override
                public void run() {
                    changes.fireChange();
                }
            }));
        }
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        ListenerRef toRemove = null;

        changesLock.lock();
        try {
            changes.removeChangeListener(listener);
            if (!changes.hasListeners()) {
                toRemove = subListenerRef;
                subListenerRef = null;
            }
        } finally {
            changesLock.unlock();
        }

        if (toRemove != null) {
            toRemove.unregister();
        }
    }
}
