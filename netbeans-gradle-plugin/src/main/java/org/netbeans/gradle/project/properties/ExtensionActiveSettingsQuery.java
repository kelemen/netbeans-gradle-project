package org.netbeans.gradle.project.properties;

import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.SingleProfileSettings;

public final class ExtensionActiveSettingsQuery implements ActiveSettingsQuery {
    private final ActiveSettingsQuery rootQuery;
    private final String extensionName;

    public ExtensionActiveSettingsQuery(ActiveSettingsQuery rootQuery, String extensionName) {
        this.rootQuery = rootQuery;
        this.extensionName = extensionName;
    }

    @Override
    public <ValueType> PropertySource<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef) {
        return rootQuery.getProperty(toExtensionDef(propertyDef, extensionName));
    }

    @Override
    public PropertySource<SingleProfileSettings> currentProfileSettings() {
        final PropertySource<SingleProfileSettings> rootSettings = rootQuery.currentProfileSettings();
        return new PropertySource<SingleProfileSettings>() {
            @Override
            public SingleProfileSettings getValue() {
                return new ExtensionSingleProfileSettings(rootSettings.getValue(), extensionName);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return rootSettings.addChangeListener(listener);
            }
        };
    }

    private static <ValueKey, ValueType> PropertyDef<ValueKey, ValueType> toExtensionDef(
            PropertyDef<ValueKey, ValueType> propertyDef, String extensionName) {
        return propertyDef.withParentConfigPath("extensions", extensionName);
    }

    private static final class ExtensionSingleProfileSettings implements SingleProfileSettings {

        private final SingleProfileSettings rootSettings;
        private final String extensionName;

        public ExtensionSingleProfileSettings(SingleProfileSettings rootSettings, String extensionName) {
            this.rootSettings = rootSettings;
            this.extensionName = extensionName;
        }

        @Override
        public ProfileKey getKey() {
            return rootSettings.getKey();
        }

        @Override
        public <ValueType> MutableProperty<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef) {
            return rootSettings.getProperty(toExtensionDef(propertyDef, extensionName));
        }
    }

}
