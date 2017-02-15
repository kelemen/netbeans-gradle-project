package org.netbeans.gradle.project.properties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;

public final class GradleLocationDistribution implements GradleLocation {
    private static final Logger LOGGER = Logger.getLogger(GradleLocationDistribution.class.getName());

    public static final String UNIQUE_TYPE_NAME = "DIST";

    private final String rawLocation;
    private final URI location;

    public GradleLocationDistribution(String location) {
        ExceptionHelper.checkNotNullArgument(location, "location");
        this.rawLocation = location;
        this.location = tryParseUri(location);
    }

    private static URI tryParseUri(String uri) {
        try {
            return new URI(StandardTaskVariable.replaceGlobalVars(uri));
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.INFO, "Invalid URI for Gradle distribution: " + uri, ex);
            return null;
        }
    }

    @Override
    public void applyLocation(Applier applier) {
        if (location != null) {
            applier.applyDistribution(location);
        }
    }

    @Override
    public String getUniqueTypeName() {
        return UNIQUE_TYPE_NAME;
    }

    @Override
    public String asString() {
        return rawLocation;
    }

    @Override
    public String toLocalizedString() {
        return NbStrings.getGradleLocationDist(location);
    }
}
