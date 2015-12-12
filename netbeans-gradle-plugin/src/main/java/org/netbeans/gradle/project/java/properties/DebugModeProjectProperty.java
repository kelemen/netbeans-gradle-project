package org.netbeans.gradle.project.java.properties;

import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.properties.global.DebugMode;

public final class DebugModeProjectProperty {
    private static final ConfigPath CONFIG_ROOT = JavaProjectProperties.PARENT_PATH.getChildPath("debug-mode", "type");

    public static final PropertyDef<?, DebugMode> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<?, DebugMode> createPropertyDef() {
        PropertyDef.Builder<DebugMode, DebugMode> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(new PropertyKeyEncodingDef<DebugMode>() {
            @Override
            public DebugMode decode(ConfigTree config) {
                String strValue = config.getValue("").trim();
                if (strValue.isEmpty()) {
                    return null;
                }

                try {
                    return DebugMode.valueOf(strValue);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }

            @Override
            public ConfigTree encode(DebugMode value) {
                return ConfigTree.singleValue(value.name());
            }
        });
        result.setValueDef(new PropertyValueDef<DebugMode, DebugMode>() {
            @Override
            public PropertySource<DebugMode> property(DebugMode valueKey) {
                return PropertyFactory.constSource(valueKey);
            }

            @Override
            public DebugMode getKeyFromValue(DebugMode value) {
                return value;
            }
        });
        return result.create();
    }


}
