package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.openide.modules.SpecificationVersion;
import org.openide.util.ChangeSupport;

public final class DefaultPropertySources {
    private static final Logger LOGGER = Logger.getLogger(DefaultPropertySources.class.getName());

    public static PropertySource<String> parseSourceLevelSource(
            final PropertySource<JavaPlatform> source,
            final boolean defaultValue) {

        if (source == null) throw new NullPointerException("source");

        return new PropertySource<String>() {
            @Override
            public String getValue() {
                return AbstractProjectProperties.getSourceLevelFromPlatform(source.getValue());
            }

            @Override
            public boolean isDefault() {
                return defaultValue;
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                source.addChangeListener(listener);
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                source.removeChangeListener(listener);
            }
        };
    }

    public static PropertySource<JavaPlatform> findPlatformSource(
            final String specName,
            final String versionStr,
            final boolean defaultValue) {

        if (specName == null) throw new NullPointerException("specName");
        if (versionStr == null) throw new NullPointerException("versionStr");

        return new JavaPlatformSource<JavaPlatform>() {
            @Override
            public boolean isDefault() {
                return defaultValue;
            }

            @Override
            protected JavaPlatform chooseFromPlatforms(JavaPlatform[] platforms) {
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
                    LOGGER.severe("Could not find any Java platform.");
                }
                else if (version.compareTo(bestMatch.getSpecification().getVersion()) > 0) {
                    LOGGER.log(Level.WARNING,
                            "The choosen platform has a higher version number than the requested one: {0}",
                            versionStr);
                }
                else {
                    LOGGER.log(Level.WARNING,
                            "The choosen platform has a lower version number than the requested one: {0}",
                            versionStr);
                }

                return bestMatch != null ? bestMatch : JavaPlatform.getDefault();
            }
        };
    }

    private static abstract class JavaPlatformSource<ValueType>
    implements
            PropertySource<ValueType> {

        private final Lock changesLock;
        private final ChangeSupport changes;
        private final PropertyChangeListener changeForwarder;

        public JavaPlatformSource() {
            this.changesLock = new ReentrantLock();
            this.changes = new ChangeSupport(this);
            this.changeForwarder = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (JavaPlatformManager.PROP_INSTALLED_PLATFORMS.equals(evt.getPropertyName())) {
                        changes.fireChange();
                    }
                }
            };
        }

        protected abstract ValueType chooseFromPlatforms(JavaPlatform[] platforms);

        @Override
        public ValueType getValue() {
            return chooseFromPlatforms(JavaPlatformManager.getDefault().getInstalledPlatforms());
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            changesLock.lock();
            try {
                boolean addedNow = !changes.hasListeners();
                changes.addChangeListener(listener);
                if (addedNow) {
                    JavaPlatformManager.getDefault().addPropertyChangeListener(changeForwarder);
                }
            } finally {
                changesLock.unlock();
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            changesLock.lock();
            try {
                if (!changes.hasListeners()) {
                    return;
                }

                changes.removeChangeListener(listener);
                if (!changes.hasListeners()) {
                    JavaPlatformManager.getDefault().removePropertyChangeListener(changeForwarder);
                }
            } finally {
                changesLock.unlock();
            }
        }
    }

    private DefaultPropertySources() {
        throw new AssertionError();
    }
}
