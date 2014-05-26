package org.netbeans.gradle.project.properties2.standard;

import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;

public final class SourceLevelProperty {
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
