package org.netbeans.gradle.project.properties.standard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties.JavaProjectPlatform;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.netbeans.gradle.project.util.EventUtils;
import org.openide.filesystems.FileObject;
import org.openide.modules.SpecificationVersion;

public final class JavaPlatformUtils {
    private static final Logger LOGGER = Logger.getLogger(JavaPlatformUtils.class.getName());

    public static ProjectPlatform getDefaultPlatform() {
        return getJavaPlatform(JavaPlatform.getDefault());
    }

    public static ProjectPlatform getJavaPlatform(JavaPlatform platform) {
        return new JavaProjectPlatform(platform);
    }

    public static JavaPlatform tryFindPlatform(String specName, String versionStr, PlatformOrder order) {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        return tryChooseFromPlatforms(specName, versionStr, order, platforms);
    }

    private static JavaPlatform tryChooseFromPlatforms(
            String specName,
            String versionStr,
            PlatformOrder order,
            JavaPlatform[] platforms) {
        return tryChooseFromPlatforms(specName, versionStr, order, Arrays.asList(platforms));
    }

    private static JavaPlatform tryChooseFromPlatforms(
            String specName,
            String versionStr,
            PlatformOrder order,
            List<JavaPlatform> platforms) {
        List<JavaPlatform> orderedPlatforms = order.orderPlatforms(platforms);
        return tryChooseFromOrderedPlatforms(specName, versionStr, orderedPlatforms);
    }

    private static String specificationToStr(Specification spec) {
        return spec.getName() + "/" + spec.getVersion();
    }

    private static String platformToStr(JavaPlatform platform) {
        Specification spec = platform.getSpecification();
        return spec != null ? specificationToStr(spec) : platform.getDisplayName() + " (null specification)";
    }

    private static String platformsToStr(Collection<? extends JavaPlatform> platforms) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        platforms.forEach(platform -> {
            result.add(platformToStr(platform));
        });
        return result.toString();
    }

    public static JavaPlatform tryChooseFromOrderedPlatforms(
            String specName,
            String versionStr,
            Collection<? extends JavaPlatform> platforms) {

        Objects.requireNonNull(specName, "specName");
        Objects.requireNonNull(versionStr, "versionStr");

        SpecificationVersion version;
        try {
            version = new SpecificationVersion(versionStr);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.INFO, "Invalid platform version: " + versionStr, ex);
            return JavaPlatform.getDefault();
        }

        for (JavaPlatform platform: platforms) {
            Specification specification = platform.getSpecification();
            if (specification != null
                    && specName.equalsIgnoreCase(specification.getName())
                    && version.equals(specification.getVersion())) {
                return platform;
            }
        }

        // We could not find an exact match, so try to find the best match:
        //
        // 1. If there is at least one platform with a version higher than
        //    requested, choose the one with the lowest version which is still
        //    higher than the requested (the closest version to the requested
        //    which is above the requested version).
        //
        // 2. In case every platform is below the requested, choose the one
        //    with the highest version number.

        JavaPlatform bestMatch = null;
        for (JavaPlatform platform: platforms) {
            Specification platformSpecification = platform.getSpecification();
            if (platformSpecification == null) {
                continue;
            }

            if (!specName.equalsIgnoreCase(platformSpecification.getName())) {
                continue;
            }

            SpecificationVersion thisVersion = platformSpecification.getVersion();
            if (thisVersion == null) {
                continue;
            }

            if (bestMatch == null) {
                bestMatch = platform;
            }
            else {
                SpecificationVersion bestVersion = bestMatch.getSpecification().getVersion();

                // required version is greater than the one we currently have
                if (version.compareTo(bestVersion) > 0) {
                    // Replace if this platform has a greater version number
                    if (bestVersion.compareTo(thisVersion) < 0) {
                        bestMatch = platform;
                    }
                }
                else {
                    // Replace if this platform is still above the requirement
                    // but is below the one we currently have.
                    if (version.compareTo(thisVersion) < 0
                            && thisVersion.compareTo(bestVersion) < 0) {
                        bestMatch = platform;
                    }
                }
            }
        }

        if (bestMatch == null) {
            LOGGER.log(Level.WARNING,
                    "Found no usable platforms in {0} returning the default platform.",
                    platformsToStr(platforms));
            return JavaPlatform.getDefault();
        }

        SpecificationVersion bestMatchVersion = bestMatch.getSpecification().getVersion();

        String higherOrLower = version.compareTo(bestMatchVersion) < 0
                ? "higher"
                : "lower";

        LOGGER.log(Level.WARNING,
                "The chosen platform has a {0} version number than the requested one: {1}. Chosen: {2}",
                new Object[]{higherOrLower, versionStr, bestMatchVersion});

        return bestMatch;
    }

    public static FileObject getHomeFolder(JavaPlatform platform) {
        Collection<FileObject> installFolders = platform.getInstallFolders();
        int numberOfFolder = installFolders.size();
        if (numberOfFolder == 0) {
            LOGGER.log(Level.WARNING, "Selected platform contains no installation folders: {0}", platform.getDisplayName());
            return null;
        }

        if (numberOfFolder > 1) {
            LOGGER.log(Level.WARNING, "Selected platform contains multiple installation folders: {0}", platform.getDisplayName());
        }

        return installFolders.iterator().next();
    }

    public static PropertySource<JavaPlatform[]> installedPlatforms() {
        return InstalledPlatformSource.INSTANCE;
    }

    private enum InstalledPlatformSource implements PropertySource<JavaPlatform[]> {
        INSTANCE;

        @Override
        public JavaPlatform[] getValue() {
            return JavaPlatformManager.getDefault().getInstalledPlatforms();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            Objects.requireNonNull(listener, "listener");

            final PropertyChangeListener propertyChangeListener = (PropertyChangeEvent evt) -> {
                if (JavaPlatformManager.PROP_INSTALLED_PLATFORMS.equals(evt.getPropertyName())) {
                    listener.run();
                }
            };

            JavaPlatformManager.getDefault().addPropertyChangeListener(propertyChangeListener);
            return EventUtils.asSafeListenerRef(() -> {
                JavaPlatformManager.getDefault().removePropertyChangeListener(propertyChangeListener);
            });
        }
    }

    private JavaPlatformUtils() {
        throw new AssertionError();
    }
}
