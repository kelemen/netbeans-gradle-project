package org.netbeans.gradle.project.properties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.properties.GradleLocation.Applier;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;

public final class GradleLocationDistribution implements GradleLocation {
    private static final Logger LOGGER = Logger.getLogger(GradleLocationDistribution.class.getName());

    public static final String UNIQUE_TYPE_NAME = "DIST";

    private final URI location;

    public GradleLocationDistribution(URI location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    public static GradleLocationRef getLocationRef(final String rawUri) {
        Objects.requireNonNull(rawUri, "rawUri");

        return new GradleLocationRef() {
            @Override
            public String getUniqueTypeName() {
                return UNIQUE_TYPE_NAME;
            }

            @Override
            public String asString() {
                return rawUri;
            }

            @Override
            public GradleLocation getLocation(StringResolver resolver) {
                String resolvedUri = resolver.resolveStringIfValid(rawUri);
                if (resolvedUri == null) {
                    return GradleLocationDefault.DEFAULT;
                }

                URI uri = tryParseUri(resolvedUri);
                if (uri == null) {
                    return GradleLocationDefault.DEFAULT;
                }

                return new GradleLocationDistribution(uri);
            }
        };
    }

    private static URI tryParseUri(String uri) {
        try {
            return new URI(StringResolvers.getDefaultGlobalResolver().resolveString(uri));
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.INFO, "Invalid URI for Gradle distribution: " + uri, ex);
            return null;
        }
    }

    @Override
    public void applyLocation(Applier applier) {
        applier.applyDistribution(location);
    }

    @Override
    public String toLocalizedString() {
        return NbStrings.getGradleLocationDist(location);
    }
}
