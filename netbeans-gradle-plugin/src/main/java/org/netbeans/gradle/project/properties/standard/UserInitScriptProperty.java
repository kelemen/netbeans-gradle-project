package org.netbeans.gradle.project.properties.standard;

import java.nio.file.Path;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;

public final class UserInitScriptProperty {
    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("user-init-script");

    private static final String CONFIG_KEY_PATH = "path";

    public static final PropertyDef<?, UserInitScriptPath> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<?, UserInitScriptPath> createPropertyDef() {
        PropertyDef.Builder<UserInitScriptPath, UserInitScriptPath> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(getKeyEncoding());
        result.setValueDef(CommonProperties.<UserInitScriptPath>getIdentityValueDef());
        return result.create();
    }

    private static UserInitScriptPath toBuildScriptPath(Path path) {
        return path != null ? new UserInitScriptPath(path) : null;
    }

    private static PropertyKeyEncodingDef<UserInitScriptPath> getKeyEncoding() {
        return new PropertyKeyEncodingDef<UserInitScriptPath>() {
            @Override
            public UserInitScriptPath decode(ConfigTree config) {
                String pathStr = config.getChildTree(CONFIG_KEY_PATH).getValue(null);
                return toBuildScriptPath(CommonProperties.tryReadFilePath(pathStr));
            }

            @Override
            public ConfigTree encode(UserInitScriptPath value) {
                String normalizeFilePath = CommonProperties.normalizeFilePath(value.getRelPath());
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.addChildBuilder(CONFIG_KEY_PATH).setValue(normalizeFilePath);
                return result.create();
            }
        };
    }
}
