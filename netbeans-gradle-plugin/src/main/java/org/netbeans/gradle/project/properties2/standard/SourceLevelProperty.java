package org.netbeans.gradle.project.properties2.standard;

import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.PropertyDef;


public final class SourceLevelProperty {
    private static final String CONFIG_KEY_SOURCE_LEVEL = "source-level";

    public static final PropertyDef<String, String> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, String> createPropertyDef() {
        PropertyDef.Builder<String, String> result
                = new PropertyDef.Builder<>(ConfigPath.fromKeys(CONFIG_KEY_SOURCE_LEVEL));
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    private SourceLevelProperty() {
        throw new AssertionError();
    }
}
