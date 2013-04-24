package org.netbeans.gradle.project.properties;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.api.event.ListenerRef;
import org.netbeans.gradle.project.api.query.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.query.ProjectPlatform;
import org.openide.modules.SpecificationVersion;
import org.openide.util.ChangeSupport;

import static org.netbeans.gradle.project.properties.ProjectPlatformSource.getJavaPlatform;

public abstract class AbstractProjectPlatformSource
implements
        PropertySource<ProjectPlatform> {

    private final Lock changesLock;
    private final ChangeSupport changes;
    private ListenerRef subListenerRef;
    private final AtomicReference<GradleProjectPlatformQuery> queryRef;

    public AbstractProjectPlatformSource() {
        this.changesLock = new ReentrantLock();
        this.changes = new ChangeSupport(this);
        this.subListenerRef = null;
        this.queryRef = new AtomicReference<GradleProjectPlatformQuery>(null);
    }

    public static ProjectPlatform getDefaultPlatform() {
        return getJavaPlatform(JavaPlatform.getDefault());
    }

    public static ProjectPlatform getJavaPlatform(JavaPlatform platform) {
        String displayName = platform.getDisplayName();
        Specification spec = platform.getSpecification();
        SpecificationVersion specVersion = spec != null ? spec.getVersion() : null;

        String name = spec != null ? spec.getName() : "";
        String version = specVersion != null ? specVersion.toString() : null;

        List<URL> bootLibs = new LinkedList<URL>();

        for (ClassPath.Entry entry: platform.getBootstrapLibraries().entries()) {
            bootLibs.add(entry.getURL());
        }

        return new ProjectPlatform(displayName, name, version, bootLibs);
    }

    protected final GradleProjectPlatformQuery getCurrentQuery() {
        return queryRef.get();
    }

    protected final void firePlatformChange() {
        changes.fireChange();
    }

    protected final GradleProjectPlatformQuery trySetQuery(GradleProjectPlatformQuery query) {
        if (query == null) throw new NullPointerException("query");

        if (queryRef.compareAndSet(null, query)) {
            ListenerRef toRemove = null;
            InitLaterListenerRef toAdd = null;

            changesLock.lock();
            try {
                if (changes.hasListeners()) {
                    toRemove = subListenerRef;
                    toAdd = new InitLaterListenerRef();
                    subListenerRef = toAdd;
                }
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
                        firePlatformChange();
                    }
                }));
                firePlatformChange();
            }
        }

        return queryRef.get();
    }

    protected abstract ProjectPlatform tryGetValue();

    @Override
    public final ProjectPlatform getValue() {
        ProjectPlatform value = tryGetValue();
        return value != null ? value : getDefaultPlatform();
    }

    @Override
    public final void addChangeListener(ChangeListener listener) {
        ListenerRef toRemove = null;
        InitLaterListenerRef toAdd = null;
        GradleProjectPlatformQuery query = queryRef.get();

        changesLock.lock();
        try {
            if (!changes.hasListeners() && query != null) {
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
            assert query != null;
            toAdd.init(query.addPlatformChangeListener(new Runnable() {
                @Override
                public void run() {
                    changes.fireChange();
                }
            }));
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener listener) {
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
