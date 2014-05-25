package org.netbeans.gradle.project.properties2.standard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.PlatformId;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.properties2.PropertyValueDef;
import org.netbeans.gradle.project.properties2.ValueMerger;
import org.netbeans.gradle.project.properties2.ValueReference;
import org.openide.modules.SpecificationVersion;

public final class TargetPlatformProperty {
    private static final String DEFAULT_PLATFORM_VERSION = getDefaultPlatformVersion("1.7");

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
            return PropertyFactory.constSource(JavaPlatform.getDefault());
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

    public static ValueMerger<JavaPlatform> getValueMerger() {
        return new ValueMerger<JavaPlatform>() {
            @Override
            public JavaPlatform mergeValues(JavaPlatform child, ValueReference<JavaPlatform> parent) {
                return child != null ? child : parent.getValue();
            }
        };
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
                ConfigTree name = config.getChildTree("target-platform-name");
                ConfigTree version = config.getChildTree("target-platform");

                return new PlatformId(
                        name.getValue(PlatformId.DEFAULT_NAME),
                        version.getValue(DEFAULT_PLATFORM_VERSION));
            }

            @Override
            public ConfigTree encode(PlatformId value) {
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.getChildBuilder("target-platform-name").setValue(value.getName());
                result.getChildBuilder("target-platform").setValue(value.getVersion());
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
