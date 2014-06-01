package org.netbeans.gradle.project.properties2.standard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.property.swing.SwingForwarderFactory;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.properties.DefaultPropertySources;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.PlatformId;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.properties2.PropertyValueDef;
import org.openide.modules.SpecificationVersion;

public final class TargetPlatformProperty {
    private static final String DEFAULT_PLATFORM_VERSION = getDefaultPlatformVersion("1.7");
    private static final String CONFIG_KEY_PLATFORM_NAME = "target-platform-name";
    private static final String CONFIG_KEY_PLATFORM_VERSION = "target-platform";

    private static final PropertyDef<PlatformId, JavaPlatform> PROPERTY_DEF = createPropertyDef();

    public static PropertySource<JavaPlatform> getProperty(ProjectProfileSettings settings) {
        List<ConfigPath> paths = Arrays.asList(
                ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_NAME),
                ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_VERSION));
        return settings.getProperty(paths, getPropertyDef());
    }

    public static PropertyDef<PlatformId, JavaPlatform> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static PropertyDef<PlatformId, JavaPlatform> createPropertyDef() {
        PropertyDef.Builder<PlatformId, JavaPlatform> result = new PropertyDef.Builder<>();
        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(getValueDef());
        result.setValueMerger(CommonProperties.<JavaPlatform>getParentIfNullValueMerger());
        return result.create();
    }

    private static SwingPropertySource<JavaPlatform[], PropertyChangeListener> javaPlatforms() {
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

    private static PropertySource<JavaPlatform> javaPlatform(final PlatformId valueKey) {
        if (valueKey == null) {
            return PropertyFactory.constSource(null);
        }

        PropertySource<JavaPlatform[]> javaPlatforms = SwingProperties.fromSwingSource(javaPlatforms(), new SwingForwarderFactory<PropertyChangeListener>() {
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

        return PropertyFactory.convert(javaPlatforms, new ValueConverter<JavaPlatform[], JavaPlatform>() {
            @Override
            public JavaPlatform convert(JavaPlatform[] input) {
                return DefaultPropertySources.tryChooseFromPlatforms(valueKey.getName(), valueKey.getVersion(), input);
            }
        });
    }

    public static PropertyValueDef<PlatformId, JavaPlatform> getValueDef() {
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

    public static PropertyKeyEncodingDef<PlatformId> getEncodingDef() {
        return new PropertyKeyEncodingDef<PlatformId>() {
            @Override
            public PlatformId decode(ConfigTree config) {
                ConfigTree name = config.getChildTree(CONFIG_KEY_PLATFORM_NAME);
                ConfigTree version = config.getChildTree(CONFIG_KEY_PLATFORM_VERSION);

                String versionStr = version.getValue(null);
                if (versionStr == null) {
                    return null;
                }
                return new PlatformId(name.getValue(PlatformId.DEFAULT_NAME), versionStr);
            }

            @Override
            public ConfigTree encode(PlatformId value) {
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.getChildBuilder(CONFIG_KEY_PLATFORM_NAME).setValue(value.getName());
                result.getChildBuilder(CONFIG_KEY_PLATFORM_VERSION).setValue(value.getVersion());
                return result.create();
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

    private TargetPlatformProperty() {
        throw new AssertionError();
    }
}
