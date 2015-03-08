package org.netbeans.gradle.project.properties2.standard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.SwingForwarderFactory;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.PlatformOrder;
import org.netbeans.gradle.project.properties.StringBasedProperty;

public final class JavaPlatformUtils {
    private static final PropertySource<List<JavaPlatform>> JAVA_PLATFORMS
            = createJavaPlatforms();

    public static PropertySource<List<JavaPlatform>> javaPlatforms() {
        return JAVA_PLATFORMS;
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
        final StringBasedProperty<PlatformOrder> order = GlobalGradleSettings.getPlatformPreferenceOrder();
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

    private JavaPlatformUtils() {
        throw new AssertionError();
    }
}
