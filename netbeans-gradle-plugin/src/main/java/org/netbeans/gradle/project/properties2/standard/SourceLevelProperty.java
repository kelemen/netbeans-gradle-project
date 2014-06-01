package org.netbeans.gradle.project.properties2.standard;

import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;

public final class SourceLevelProperty {
    private static final PropertyDef<String, String> PROPERTY_DEF = createPropertyDef();

    public static PropertyDef<String, String> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static PropertyDef<String, String> createPropertyDef() {
        PropertyDef.Builder<String, String> result = new PropertyDef.Builder<>();
        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    public static PropertyKeyEncodingDef<String> getEncodingDef() {
        return new PropertyKeyEncodingDef<String>() {
            @Override
            public String decode(ConfigTree config) {
                return config.getValue(null);
            }

            @Override
            public ConfigTree encode(String value) {
                return ConfigTree.singleValue(value);
            }
        };
    }

    private SourceLevelProperty() {
        throw new AssertionError();
    }
}
