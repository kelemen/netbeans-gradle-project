package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.openide.util.Lookup;

public final class ProjectPlatformSource extends AbstractProjectPlatformSource {
    private final String name;
    private final String version;
    private final boolean defaultValue;

    public ProjectPlatformSource(String name, String version, boolean defaultValue) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        ExceptionHelper.checkNotNullArgument(version, "version");

        this.name = name;
        this.version = version;
        this.defaultValue = defaultValue;
    }

    private GradleProjectPlatformQuery findOwnerQuery() {
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            if (query.isOwnerQuery(name)) {
                return query;
            }
        }
        return null;
    }

    private ProjectPlatform findPlatformFromAll() {
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            ProjectPlatform platform = query.tryFindPlatformByName(name, version);
            if (platform != null) {
                return platform;
            }
        }
        return null;
    }

    @Override
    public ProjectPlatform tryGetValue() {
        GradleProjectPlatformQuery query = getCurrentQuery();
        if (query == null) {
            query = findOwnerQuery();
            if (query == null) {
                return findPlatformFromAll();
            }

            query = trySetQuery(query);
        }
        return query.tryFindPlatformByName(name, version);
    }

    @Override
    public boolean isDefault() {
        return defaultValue;
    }
}
