package org.netbeans.gradle.project.properties.standard;

import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;

public final class ProjectDisplayNameProperty {
    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("appearance", "display-name-pattern");

    public static final PropertyDef<String, String> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, String> createPropertyDef() {
        PropertyDef.Builder<String, String> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    public static PropertySource<String> defaultValue() {
        return GlobalGradleSettings.getDefault().displayNamePattern();
    }

    private ProjectDisplayNameProperty() {
        throw new AssertionError();
    }
}
