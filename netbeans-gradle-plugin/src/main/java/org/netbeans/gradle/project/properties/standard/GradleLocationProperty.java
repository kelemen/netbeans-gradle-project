package org.netbeans.gradle.project.properties.standard;

import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties.ConfigPath;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.PropertyDef;
import org.netbeans.gradle.project.properties.PropertyValueDef;

public final class GradleLocationProperty {
    private static final String CONFIG_KEY_GRADLE_LOCATION = "gradle-home";

    public static final PropertyDef<?, GradleLocationDef> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<?, GradleLocationDef> createPropertyDef() {
        PropertyDef.Builder<String, GradleLocationDef> result
                = new PropertyDef.Builder<>(ConfigPath.fromKeys(CONFIG_KEY_GRADLE_LOCATION));
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
