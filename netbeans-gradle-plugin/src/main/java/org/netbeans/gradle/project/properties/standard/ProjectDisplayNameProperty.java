package org.netbeans.gradle.project.properties.standard;

import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.view.DisplayableTaskVariable;

public final class ProjectDisplayNameProperty {
    public static final String DEFAULT_VALUE = DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant();

    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("appearance", "display-name-pattern");

    public static final PropertyDef<String, String> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, String> createPropertyDef() {
        PropertyDef.Builder<String, String> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    private ProjectDisplayNameProperty() {
        throw new AssertionError();
    }
}
