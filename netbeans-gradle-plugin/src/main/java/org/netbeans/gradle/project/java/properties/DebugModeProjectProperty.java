package org.netbeans.gradle.project.java.properties;

import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.properties.standard.CommonProperties;

public final class DebugModeProjectProperty {
    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("debug-mode", "type");

    public static final DebugMode DEFAULT = DebugMode.DEBUGGER_ATTACHES;
    public static final PropertyDef<?, DebugMode> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<?, DebugMode> createPropertyDef() {
        PropertyDef.Builder<DebugMode, DebugMode> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(CommonProperties.enumKeyEncodingDef(DebugMode.class));
        result.setValueDef(CommonProperties.<DebugMode>getIdentityValueDef());
        return result.create();
    }
}
