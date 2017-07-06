package org.netbeans.gradle.project.properties;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.netbeans.gradle.project.properties.standard.JavaPlatformUtils;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = GradleProjectPlatformQuery.class, position = 1000)
public final class DefaultGradleProjectPlatformQuery
implements
        GradleProjectPlatformQuery {
    private final Set<String> knownPlatforms;

    public DefaultGradleProjectPlatformQuery() {
        this.knownPlatforms = new HashSet<>(Arrays.asList("j2se"));
    }

    private static PropertySource<PlatformOrder> orderProperty() {
        return CommonGlobalSettings.getDefault().platformPreferenceOrder().getActiveSource();
    }

    @Override
    public ListenerRef addPlatformChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");

        return ListenerRefs.combineListenerRefs(
                JavaPlatformUtils.installedPlatforms().addChangeListener(listener),
                orderProperty().addChangeListener(listener));
    }

    @Override
    public boolean isOwnerQuery(String platformName) {
        return knownPlatforms.contains(platformName);
    }

    @Override
    public Collection<ProjectPlatform> getAvailablePlatforms() {
        JavaPlatform[] allPlatforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        List<JavaPlatform> platforms = orderProperty().getValue().filterIndistinguishable(allPlatforms);
        List<ProjectPlatform> result = new ArrayList<>(platforms.size());

        for (JavaPlatform platform: platforms) {
            result.add(JavaPlatformUtils.getJavaPlatform(platform));
        }
        return result;
    }

    @Override
    public ProjectPlatform tryFindPlatformByName(String name, String version) {
        JavaPlatform platform = JavaPlatformUtils.tryFindPlatform(name, version, orderProperty().getValue());
        return platform != null
                ? JavaPlatformUtils.getJavaPlatform(platform)
                : null;
    }

    @Override
    public ProjectPlatform tryFindPlatformByUri(URI uri) {
        return null;
    }
}
