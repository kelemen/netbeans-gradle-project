package org.netbeans.gradle.project.properties2.standard;

import java.util.Arrays;
import java.util.List;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;


public final class SourceLevelProperty {
    private static final PropertyDef<String, String> PROPERTY_DEF = createPropertyDef();
    private static final String CONFIG_KEY_SOURCE_LEVEL = "source-level";

    public static PropertySource<String> getProperty(ProjectProfileSettings settings) {
        List<ConfigPath> paths = Arrays.asList(ConfigPath.fromKeys(CONFIG_KEY_SOURCE_LEVEL));
        return settings.getProperty(paths, getPropertyDef());
    }

    public static PropertyDef<String, String> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static PropertyDef<String, String> createPropertyDef() {
        PropertyDef.Builder<String, String> result = new PropertyDef.Builder<>();
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    private SourceLevelProperty() {
        throw new AssertionError();
    }
}
