package org.netbeans.gradle.project.java.query;

import java.util.Objects;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.standard.JavaPlatformUtils;
import org.netbeans.gradle.project.query.J2SEPlatformFromScriptQuery;
import org.openide.util.Lookup;

public final class J2SEPlatformFromScriptQueryImpl
implements
        J2SEPlatformFromScriptQuery {

    private final JavaExtension javaExt;

    public J2SEPlatformFromScriptQueryImpl(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
    }

    private static GradleProjectPlatformQuery findOwnerQuery(String name) {
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            if (query.isOwnerQuery(name)) {
                return query;
            }
        }
        return null;
    }

    private ProjectPlatform findPlatformFromAll(String name, String version) {
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            ProjectPlatform platform = query.tryFindPlatformByName(name, version);
            if (platform != null) {
                return platform;
            }
        }
        return null;
    }

    private ProjectPlatform tryFindPlatform(String name, String version) {
        GradleProjectPlatformQuery query = findOwnerQuery(name);
        return query != null
                ? query.tryFindPlatformByName(name, version)
                : findPlatformFromAll(name, version);
    }

    @Override
    public ProjectPlatform getPlatform() {
        String targetLevel = javaExt.getCurrentModel().getMainModule().getCompatibilityModel().getTargetCompatibility();

        ProjectPlatform result = tryFindPlatform("j2se", targetLevel);
        return result != null ? result : JavaPlatformUtils.getDefaultPlatform();
    }

    @Override
    public String getSourceLevel() {
        return javaExt.getCurrentModel().getMainModule().getCompatibilityModel().getSourceCompatibility();
    }
}
