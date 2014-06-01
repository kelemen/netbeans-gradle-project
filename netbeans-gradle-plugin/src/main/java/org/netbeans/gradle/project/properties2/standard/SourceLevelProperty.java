package org.netbeans.gradle.project.properties2.standard;

import java.util.Arrays;
import java.util.List;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;


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
        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    private static PropertyKeyEncodingDef<String> getEncodingDef() {
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
