package org.netbeans.gradle.project.properties.standard;

import java.nio.file.Path;
import org.netbeans.gradle.project.properties.ConfigPath;
import org.netbeans.gradle.project.properties.ConfigTree;
import org.netbeans.gradle.project.properties.PropertyDef;
import org.netbeans.gradle.project.properties.PropertyKeyEncodingDef;

public final class UserBuildScriptProperty {
    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("user-build-script");

    private static final String CONFIG_KEY_PATH = "path";

    public static final PropertyDef<?, UserBuildScriptPath> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<?, UserBuildScriptPath> createPropertyDef() {
        PropertyDef.Builder<UserBuildScriptPath, UserBuildScriptPath> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(getKeyEncoding());
        result.setValueDef(CommonProperties.<UserBuildScriptPath>getIdentityValueDef());
        return result.create();
    }

    private static UserBuildScriptPath toBuildScriptPath(Path path) {
        return path != null ? new UserBuildScriptPath(path) : null;
    }

    private static PropertyKeyEncodingDef<UserBuildScriptPath> getKeyEncoding() {
        return new PropertyKeyEncodingDef<UserBuildScriptPath>() {
            @Override
            public UserBuildScriptPath decode(ConfigTree config) {
                String pathStr = config.getChildTree(CONFIG_KEY_PATH).getValue(null);
                return toBuildScriptPath(CommonProperties.tryReadFilePath(pathStr));
            }

            @Override
            public ConfigTree encode(UserBuildScriptPath value) {
                String normalizeFilePath = CommonProperties.normalizeFilePath(value.getRelPath());
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.addChildBuilder(CONFIG_KEY_PATH).setValue(normalizeFilePath);
                return result.create();
            }
        };
    }
}
