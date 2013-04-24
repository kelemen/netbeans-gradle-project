package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.api.query.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.query.ProjectPlatform;

public final class ProjectQueryPlatformSource extends AbstractProjectPlatformSource {
    private final String name;
    private final String version;
    private final boolean defaultValue;

    public ProjectQueryPlatformSource(
            GradleProjectPlatformQuery query,
            String name,
            String version,
            boolean defaultValue) {
        if (query == null) throw new NullPointerException("query");
        if (name == null) throw new NullPointerException("name");
        if (version == null) throw new NullPointerException("version");

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
