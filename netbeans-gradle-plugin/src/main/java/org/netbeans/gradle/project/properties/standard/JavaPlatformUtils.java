package org.netbeans.gradle.project.properties.standard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.property.swing.SwingForwarderFactory;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.properties.JavaProjectPlatform;
import org.netbeans.gradle.project.properties.PropertyValueDef;
import org.netbeans.gradle.project.properties.StringBasedProperty;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.openide.modules.SpecificationVersion;

public final class JavaPlatformUtils {
    private static final Logger LOGGER = Logger.getLogger(JavaPlatformUtils.class.getName());

    public static final String DEFAULT_PLATFORM_VERSION = getDefaultPlatformVersion("1.7");

    private static final PropertySource<List<JavaPlatform>> JAVA_PLATFORMS
            = createJavaPlatforms();

    public static ProjectPlatform getDefaultPlatform() {
        return getJavaPlatform(JavaPlatform.getDefault());
    }

    public static ProjectPlatform getJavaPlatform(JavaPlatform platform) {
        return new JavaProjectPlatform(platform);
    }

    public static PropertySource<List<JavaPlatform>> javaPlatforms() {
        return JAVA_PLATFORMS;
    }

    public static PropertyValueDef<PlatformId, JavaPlatform> getPlatformIdValueDef() {
        return new PropertyValueDef<PlatformId, JavaPlatform>() {
            @Override
            public PropertySource<JavaPlatform> property(PlatformId valueKey) {
                return javaPlatform(valueKey);
            }

            @Override
            public PlatformId getKeyFromValue(JavaPlatform value) {
                Specification specification = value != null
                        ? value.getSpecification()
                        : null;

                if (specification == null) {
                    return null;
                }

                String name = specification.getName();
                String version = specification.getVersion().toString();
                return new PlatformId(name, version);
            }
        };
    }

    private static PropertySource<JavaPlatform> javaPlatform(final PlatformId valueKey) {
        if (valueKey == null) {
            return PropertyFactory.constSource(null);
        }

        PropertySource<List<JavaPlatform>> javaPlatforms = javaPlatforms();

        return PropertyFactory.convert(javaPlatforms, new ValueConverter<List<JavaPlatform>, JavaPlatform>() {
            @Override
            public JavaPlatform convert(List<JavaPlatform> input) {
                return tryChooseFromPlatforms(valueKey.getName(), valueKey.getVersion(), input);
            }
        });
    }

    private static SwingPropertySource<JavaPlatform[], PropertyChangeListener> rawUnorderedJavaPlatforms() {
        final JavaPlatformManager platformManager = JavaPlatformManager.getDefault();

        return new SwingPropertySource<JavaPlatform[], PropertyChangeListener>() {
            @Override
            public JavaPlatform[] getValue() {
                return JavaPlatformManager.getDefault().getInstalledPlatforms();
            }

            @Override
            public void addChangeListener(PropertyChangeListener listener) {
                platformManager.addPropertyChangeListener(listener);
            }

            @Override
            public void removeChangeListener(PropertyChangeListener listener) {
                platformManager.removePropertyChangeListener(listener);
            }
        };
    }

    private static PropertySource<JavaPlatform[]> unorderedJavaPlatforms() {
        return SwingProperties.fromSwingSource(rawUnorderedJavaPlatforms(), new SwingForwarderFactory<PropertyChangeListener>() {
            @Override
            public PropertyChangeListener createForwarder(final Runnable listener) {
                return new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (JavaPlatformManager.PROP_INSTALLED_PLATFORMS.equals(evt.getPropertyName())) {
                            listener.run();
                        }
                    }
                };
            }
        });
    }

    private static PropertySource<List<JavaPlatform>> createJavaPlatforms() {
        final PropertySource<JavaPlatform[]> unordered = unorderedJavaPlatforms();
        final StringBasedProperty<PlatformOrder> order = GlobalGradleSettings.getDefault().platformPreferenceOrder();
        return new PropertySource<List<JavaPlatform>>() {
            @Override
            public List<JavaPlatform> getValue() {
                JavaPlatform[] platforms = unordered.getValue();
                return GlobalGradleSettings.orderPlatforms(platforms);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                return ListenerRegistries.combineListenerRefs(
                        order.addChangeListener(listener),
                        unordered.addChangeListener(listener));
            }
        };
    }

    private static String getDefaultPlatformVersion(String defaultVersion) {
        JavaPlatform platform = JavaPlatform.getDefault();
        if (platform == null) {
            return defaultVersion;
        }

        Specification specification = platform.getSpecification();
        if (specification == null) {
            return defaultVersion;
        }

        SpecificationVersion version = specification.getVersion();
        if (version == null) {
            return defaultVersion;
        }

        return version.toString();
    }

    public static JavaPlatform tryFindPlatform(String specName, String versionStr) {
        return tryChooseFromPlatforms(specName, versionStr, JavaPlatformManager.getDefault().getInstalledPlatforms());
    }

    public static JavaPlatform tryChooseFromPlatforms(
            String specName,
            String versionStr,
            JavaPlatform[] platforms) {
        return tryChooseFromPlatforms(specName, versionStr, Arrays.asList(platforms));
    }

    public static JavaPlatform tryChooseFromPlatforms(
            String specName,
            String versionStr,
            List<JavaPlatform> platforms) {
        List<JavaPlatform> orderedPlatforms = GlobalGradleSettings.orderPlatforms(platforms);
        return tryChooseFromOrderedPlatforms(specName, versionStr, orderedPlatforms);
    }

    private static JavaPlatform tryChooseFromOrderedPlatforms(
            String specName,
            String versionStr,
            Collection<JavaPlatform> platforms) {

        ExceptionHelper.checkNotNullArgument(specName, "specName");
        ExceptionHelper.checkNotNullArgument(versionStr, "versionStr");

        SpecificationVersion version;
        try {
            version = new SpecificationVersion(versionStr);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.INFO, "Invalid platform version: " + versionStr, ex);
            return JavaPlatform.getDefault();
        }

        for (JavaPlatform platform: platforms) {
            Specification specification = platform.getSpecification();
            if (specName.equalsIgnoreCase(specification.getName())
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

    public static PropertySource<JavaPlatform> findPlatformSource(
            final String specName,
            final String versionStr) {

        ExceptionHelper.checkNotNullArgument(specName, "specName");
        ExceptionHelper.checkNotNullArgument(versionStr, "versionStr");

        return new JavaPlatformSource<JavaPlatform>() {
            @Override
            protected JavaPlatform chooseFromPlatforms(JavaPlatform[] platforms) {
                JavaPlatform bestMatch = tryChooseFromPlatforms(specName, versionStr, platforms);

                if (bestMatch == null) {
                    LOGGER.severe("Could not find any Java platform.");
                    return JavaPlatform.getDefault();
                }
                else {
                    return bestMatch;
                }
            }
        };
    }

    private static final class InstalledPlatformSource implements PropertySource<JavaPlatform[]> {
        @Override
        public JavaPlatform[] getValue() {
            return JavaPlatformManager.getDefault().getInstalledPlatforms();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (JavaPlatformManager.PROP_INSTALLED_PLATFORMS.equals(evt.getPropertyName())) {
                        listener.run();
                    }
                }
            };

            JavaPlatformManager.getDefault().addPropertyChangeListener(propertyChangeListener);
            return NbListenerRefs.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    JavaPlatformManager.getDefault().removePropertyChangeListener(propertyChangeListener);
                }
            });
        }
    }

    private static abstract class JavaPlatformSource<ValueType>
    implements
            PropertySource<ValueType> {

        private final PropertySource<JavaPlatform[]> installedPlatforms;

        public JavaPlatformSource() {
            this.installedPlatforms = new InstalledPlatformSource();
        }

        protected abstract ValueType chooseFromPlatforms(JavaPlatform[] platforms);

        @Override
        public ValueType getValue() {
            return chooseFromPlatforms(JavaPlatformManager.getDefault().getInstalledPlatforms());
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return ListenerRegistries.combineListenerRefs(
                    GlobalGradleSettings.getDefault().platformPreferenceOrder().addChangeListener(listener),
                    installedPlatforms.addChangeListener(listener));
        }
    }

    private JavaPlatformUtils() {
        throw new AssertionError();
    }
}
