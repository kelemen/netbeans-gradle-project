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
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.properties2.standard.JavaPlatformUtils;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = GradleProjectPlatformQuery.class, position = 1000)
public final class DefaultGradleProjectPlatformQuery
implements
        GradleProjectPlatformQuery {
    private final Set<String> knownPlatforms;

    public DefaultGradleProjectPlatformQuery() {
        this.knownPlatforms = new HashSet<>(Arrays.asList("j2se"));
    }

    @Override
    public NbListenerRef addPlatformChangeListener(final Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

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

        StringBasedProperty<PlatformOrder> order
                = GlobalGradleSettings.getPlatformPreferenceOrder();

        final ListenerRef orderListenerRef = order.addChangeListener(listener);

        return NbListenerRefs.fromRunnable(new Runnable() {
            @Override
            public void run() {
                manager.removePropertyChangeListener(changeListener);
                orderListenerRef.unregister();
            }
        });
    }

    @Override
    public boolean isOwnerQuery(String platformName) {
        return knownPlatforms.contains(platformName);
    }

    @Override
    public Collection<ProjectPlatform> getAvailablePlatforms() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        List<ProjectPlatform> result = new ArrayList<>(platforms.length);

        for (JavaPlatform platform: GlobalGradleSettings.filterIndistinguishable(platforms)) {
            result.add(JavaPlatformUtils.getJavaPlatform(platform));
        }
        return result;
    }

    @Override
    public ProjectPlatform tryFindPlatformByName(String name, String version) {
        JavaPlatform platform = JavaPlatformUtils.tryFindPlatform(name, version);
        return platform != null
                ? JavaPlatformUtils.getJavaPlatform(platform)
                : null;
    }

    @Override
    public ProjectPlatform tryFindPlatformByUri(URI uri) {
        return null;
    }
}
