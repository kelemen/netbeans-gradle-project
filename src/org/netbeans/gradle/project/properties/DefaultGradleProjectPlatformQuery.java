package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.api.event.ListenerRef;
import org.netbeans.gradle.project.api.query.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.query.ProjectPlatform;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = GradleProjectPlatformQuery.class, position = 1000)
public final class DefaultGradleProjectPlatformQuery
implements
        GradleProjectPlatformQuery {
    private final Set<String> knownPlatforms;

    public DefaultGradleProjectPlatformQuery() {
        this.knownPlatforms = new HashSet<String>(Arrays.asList("j2se"));
    }

    @Override
    public ListenerRef addPlatformChangeListener(final Runnable listener) {
        if (listener == null) throw new NullPointerException("listener");

        final PropertyChangeListener changeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (JavaPlatformManager.PROP_INSTALLED_PLATFORMS.equals(evt.getPropertyName())) {
                    listener.run();
                }
            }
        };

        final JavaPlatformManager manager = JavaPlatformManager.getDefault();
        manager.addPropertyChangeListener(changeListener);

        return new ListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                manager.removePropertyChangeListener(changeListener);
                registered = false;
            }
        };
    }

    @Override
    public boolean isOwnerQuery(String platformName) {
        return knownPlatforms.contains(platformName);
    }

    @Override
    public Collection<ProjectPlatform> getAvailablePlatforms() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        List<ProjectPlatform> result = new ArrayList<ProjectPlatform>(platforms.length);

        for (final JavaPlatform platform: platforms) {
            result.add(AbstractProjectPlatformSource.getJavaPlatform(platform));
        }
        return result;
    }

    @Override
    public ProjectPlatform tryFindPlatformByName(String name, String version) {
        JavaPlatform platform = DefaultPropertySources.tryFindPlatform(name, version);
        return platform != null
                ? AbstractProjectPlatformSource.getJavaPlatform(platform)
                : null;
    }

    @Override
    public ProjectPlatform tryFindPlatformByUri(URI uri) {
        return null;
    }
}
