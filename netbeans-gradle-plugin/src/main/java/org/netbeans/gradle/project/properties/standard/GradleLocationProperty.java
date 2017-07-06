package org.netbeans.gradle.project.properties.standard;

import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.properties.GradleLocationDef;

public final class GradleLocationProperty {
    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("gradle-home");

    public static final PropertyDef<?, GradleLocationDef> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<?, GradleLocationDef> createPropertyDef() {
        PropertyDef.Builder<String, GradleLocationDef> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(getValueDef());
        return result.create();
    }

    private static GradleLocationDef tryGetGradleLocationFromString(String gradleLocation) {
        return gradleLocation != null
                ? GradleLocationDef.parseFromString(gradleLocation)
                : null;
    }

    private static PropertyValueDef<String, GradleLocationDef> getValueDef() {
        return new PropertyValueDef<String, GradleLocationDef>() {
            @Override
            public PropertySource<GradleLocationDef> property(String valueKey) {
                return PropertyFactory.constSource(tryGetGradleLocationFromString(valueKey));
            }

            @Override
            public String getKeyFromValue(GradleLocationDef value) {
                return value != null
                        ? value.toStringFormat()
                        : null;
            }
        };
    }

    private GradleLocationProperty() {
        throw new AssertionError();
    }
}
