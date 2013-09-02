package org.netbeans.gradle.project.java.query;

import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.ProjectPlatformSource;
import org.netbeans.gradle.project.query.J2SEPlatformFromScriptQuery;

public final class J2SEPlatformFromScriptQueryImpl
implements
        J2SEPlatformFromScriptQuery {

    private final JavaExtension javaExt;

    public J2SEPlatformFromScriptQueryImpl(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
    }

    @Override
    public ProjectPlatform getPlatform() {
        String targetLevel = javaExt.getCurrentModel().getMainModule().getCompatibilityModel().getTargetCompatibility();
        return new ProjectPlatformSource("j2se", targetLevel, true).getValue();
    }

    @Override
    public String getSourceLevel() {
        return javaExt.getCurrentModel().getMainModule().getCompatibilityModel().getSourceCompatibility();
    }
}
