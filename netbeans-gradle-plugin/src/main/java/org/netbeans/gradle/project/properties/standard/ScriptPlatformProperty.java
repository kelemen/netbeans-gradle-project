package org.netbeans.gradle.project.properties.standard;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties.ConfigPath;
import org.netbeans.gradle.project.properties.ConfigTree;
import org.netbeans.gradle.project.properties.PropertyDef;
import org.netbeans.gradle.project.properties.PropertyKeyEncodingDef;

public final class ScriptPlatformProperty {
    private static final ConfigPath CONFIG_KEY_SCRIPT_PLATFORM = ConfigPath.fromKeys("script-platform");

    private static final String GENERIC_PLATFORM_NAME_NODE = "spec-name";
    private static final String GENERIC_PLATFORM_VERSION_NODE = "spec-version";

    public static final PropertyDef<PlatformId, JavaPlatform> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<PlatformId, JavaPlatform> createPropertyDef() {
        PropertyDef.Builder<PlatformId, JavaPlatform> result
                = new PropertyDef.Builder<>(CONFIG_KEY_SCRIPT_PLATFORM);

        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(JavaPlatformUtils.getPlatformIdValueDef());
        return result.create();
    }

    private static PropertyKeyEncodingDef<PlatformId> getEncodingDef() {
        return new PropertyKeyEncodingDef<PlatformId>() {
            @Override
            public PlatformId decode(ConfigTree config) {
                ConfigTree name = config.getChildTree(GENERIC_PLATFORM_NAME_NODE);
                ConfigTree version = config.getChildTree(GENERIC_PLATFORM_VERSION_NODE);

                String versionStr = version.getValue(null);
                if (versionStr == null) {
                    return null;
                }
                return new PlatformId(name.getValue(PlatformId.DEFAULT_NAME), versionStr);
            }

            @Override
            public ConfigTree encode(PlatformId value) {
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.getChildBuilder(GENERIC_PLATFORM_NAME_NODE).setValue(value.getName());
                result.getChildBuilder(GENERIC_PLATFORM_VERSION_NODE).setValue(value.getVersion());
                return result.create();
            }
        };
    }

    private ScriptPlatformProperty() {
        throw new AssertionError();
    }
}
