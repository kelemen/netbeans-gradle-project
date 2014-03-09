package org.netbeans.gradle.project.properties2;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.openide.modules.SpecificationVersion;

public final class StandardProperties {
    private static final String DEFAULT_PLATFORM_VERSION = getDefaultPlatformVersion("1.7");

    static PropertyKeyEncodingDef<PlatformId> getTargetPlatformEncodingDef() {
        return new PropertyKeyEncodingDef<PlatformId>() {
            @Override
            public PlatformId decode(ConfigTree config) {
                ConfigTree name = config.getChildTree("target-platform-name");
                ConfigTree version = config.getChildTree("target-platform");

                return new PlatformId(
                        name.getValue(PlatformId.DEFAULT_NAME),
                        version.getValue(DEFAULT_PLATFORM_VERSION));
            }

            @Override
            public ConfigTree encode(PlatformId value) {
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.getChildBuilder("target-platform-name").setValue(value.getName());
                result.getChildBuilder("target-platform").setValue(value.getVersion());
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

    private StandardProperties() {
        throw new AssertionError();
    }
}
