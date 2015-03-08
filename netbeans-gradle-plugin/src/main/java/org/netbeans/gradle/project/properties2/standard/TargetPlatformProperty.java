package org.netbeans.gradle.project.properties2.standard;

import java.util.Arrays;
import java.util.List;
import org.jtrim.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;
import org.openide.modules.SpecificationVersion;

public final class TargetPlatformProperty {
    private static final String DEFAULT_PLATFORM_VERSION = getDefaultPlatformVersion("1.7");
    private static final String CONFIG_KEY_PLATFORM_NAME = "target-platform-name";
    private static final String CONFIG_KEY_PLATFORM_VERSION = "target-platform";

    private static final PropertyDef<PlatformId, JavaPlatform> PROPERTY_DEF = createPropertyDef();

    public static PropertySource<JavaPlatform> getProperty(ProjectProfileSettings settings) {
        List<ConfigPath> paths = Arrays.asList(
                ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_NAME),
                ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_VERSION));
        return settings.getProperty(paths, getPropertyDef());
    }

    public static PropertyDef<PlatformId, JavaPlatform> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static PropertyDef<PlatformId, JavaPlatform> createPropertyDef() {
        PropertyDef.Builder<PlatformId, JavaPlatform> result = new PropertyDef.Builder<>();
        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(JavaPlatformUtils.getPlatformIdValueDef());
        return result.create();
    }

    private static PropertyKeyEncodingDef<PlatformId> getEncodingDef() {
        return new PropertyKeyEncodingDef<PlatformId>() {
            @Override
            public PlatformId decode(ConfigTree config) {
                ConfigTree name = config.getChildTree(CONFIG_KEY_PLATFORM_NAME);
                ConfigTree version = config.getChildTree(CONFIG_KEY_PLATFORM_VERSION);

                String versionStr = version.getValue(null);
                if (versionStr == null) {
                    return null;
                }
                return new PlatformId(name.getValue(PlatformId.DEFAULT_NAME), versionStr);
            }

            @Override
            public ConfigTree encode(PlatformId value) {
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.getChildBuilder(CONFIG_KEY_PLATFORM_NAME).setValue(value.getName());
                result.getChildBuilder(CONFIG_KEY_PLATFORM_VERSION).setValue(value.getVersion());
                return result.create();
            }
        };
    }

    private static String getDefaultPlatformVersion(String defaultVersion) {
        JavaPlatform platform = JavaPlatform.getDefault();
        if (platform == null) {
            return defaultVersion;
        }

        Specification specification = platform.getSpecification();
        if (specification == null) {
            return defaultVersion;
        }

        SpecificationVersion version = specification.getVersion();
        if (version == null) {
            return defaultVersion;
        }

        return version.toString();
    }

    private TargetPlatformProperty() {
        throw new AssertionError();
    }
}
