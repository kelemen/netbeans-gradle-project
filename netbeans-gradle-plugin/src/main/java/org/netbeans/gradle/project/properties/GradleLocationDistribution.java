package org.netbeans.gradle.project.properties;

import java.net.URI;
import org.netbeans.gradle.project.NbGradleProject;

public final class GradleLocationDistribution implements GradleLocation {
    public static final String UNIQUE_TYPE_NAME = "DIST";

    private final URI location;

    public GradleLocationDistribution(URI location) {
        if (location == null) throw new NullPointerException("location");
        this.location = location;
    }

    public URI getLocation() {
        return location;
    }

    @Override
    public void applyLocation(NbGradleProject project, Applier applier) {
        applier.applyDistribution(location);
    }

    @Override
    public String getUniqueTypeName() {
        return UNIQUE_TYPE_NAME;
    }

    @Override
    public String asString() {
        return location.toString();
    }
}
