package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;

public final class ProjectQueryPlatformSource extends AbstractProjectPlatformSource {
    private final String name;
    private final String version;
    private final boolean defaultValue;

    public ProjectQueryPlatformSource(
            GradleProjectPlatformQuery query,
            String name,
            String version,
            boolean defaultValue) {
        ExceptionHelper.checkNotNullArgument(query, "query");
        ExceptionHelper.checkNotNullArgument(name, "name");
        ExceptionHelper.checkNotNullArgument(version, "version");

        this.name = name;
        this.version = version;
        this.defaultValue = defaultValue;

        trySetQuery(query);
    }

    @Override
    public ProjectPlatform tryGetValue() {
        return getCurrentQuery().tryFindPlatformByName(name, version);
    }

    @Override
    public boolean isDefault() {
        return defaultValue;
    }
}
