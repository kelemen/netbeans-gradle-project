package org.netbeans.gradle.project.properties;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ProxyListenerRegistry;
import org.jtrim.event.SimpleListenerRegistry;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;

import static org.netbeans.gradle.project.properties.AbstractProjectPlatformSource.getJavaPlatform;

public abstract class AbstractProjectPlatformSource
implements
        PropertySource<ProjectPlatform> {

    private final ProxyListenerRegistry<Runnable> listeners;
    private final AtomicReference<GradleProjectPlatformQuery> queryRef;

    public AbstractProjectPlatformSource() {
        this.queryRef = new AtomicReference<>(null);
        this.listeners = new ProxyListenerRegistry<>(NoOpListenerRegistry.INSTANCE);
    }

    public static ProjectPlatform getDefaultPlatform() {
        return getJavaPlatform(JavaPlatform.getDefault());
    }

    public static ProjectPlatform getJavaPlatform(JavaPlatform platform) {
        return new JavaProjectPlatform(platform);
    }

    protected final GradleProjectPlatformQuery getCurrentQuery() {
        return queryRef.get();
    }

    protected final void firePlatformChange() {
        listeners.onEvent(EventListeners.runnableDispatcher(), null);
    }

    private static SimpleListenerRegistry<Runnable> asListenerRegistry(final GradleProjectPlatformQuery query) {
        return new SimpleListenerRegistry<Runnable>() {
            @Override
            public ListenerRef registerListener(Runnable listener) {
                return query.addPlatformChangeListener(listener);
            }
        };
    }

    protected final GradleProjectPlatformQuery trySetQuery(GradleProjectPlatformQuery query) {
        ExceptionHelper.checkNotNullArgument(query, "query");

        if (queryRef.compareAndSet(null, query)) {
            listeners.replaceRegistry(asListenerRegistry(query));
            firePlatformChange();
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
    public final ListenerRef addChangeListener(Runnable listener) {
        return listeners.registerListener(listener);
    }

    private enum NoOpListenerRegistry implements SimpleListenerRegistry<Runnable> {
        INSTANCE;

        @Override
        public ListenerRef registerListener(Runnable listener) {
            return UnregisteredListenerRef.INSTANCE;
        }
    }
}
