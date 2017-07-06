package org.netbeans.gradle.project.properties.standard;

import java.util.Arrays;
import java.util.Objects;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.properties.PlatformSelector;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.PlatformOrder;

import static org.netbeans.gradle.project.properties.standard.JavaPlatformUtils.*;

public final class ScriptPlatformProperty {
    private static final ConfigPath CONFIG_KEY_SCRIPT_PLATFORM = ConfigPath.fromKeys("script-platform");

    public static PropertyDef<?, ScriptPlatform> getPropertyDef(PropertyReference<? extends PlatformOrder> orderRef) {
        return getPropertyDef(orderRef.getActiveSource());
    }

    public static PropertyDef<?, ScriptPlatform> getPropertyDef(PropertySource<? extends PlatformOrder> orderRef) {
        PropertyDef.Builder<PlatformSelector, ScriptPlatform> result
                = new PropertyDef.Builder<>(CONFIG_KEY_SCRIPT_PLATFORM);

        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(getPlatformIdValueDef(orderRef));
        return result.create();
    }

    private static PropertyKeyEncodingDef<PlatformSelector> getEncodingDef() {
        return new PropertyKeyEncodingDef<PlatformSelector>() {
            @Override
            public PlatformSelector decode(ConfigTree config) {
                PlatformSelector result = ExplicitPlatformRef.tryParse(config);
                if (result != null) {
                    return result;
                }

                return PlatformId.tryDecode(config);
            }

            @Override
            public ConfigTree encode(PlatformSelector value) {
                return value.toConfig();
            }
        };
    }

    private static PropertySource<ScriptPlatform> javaPlatform(
            final PlatformSelector selector,
            final PropertySource<? extends PlatformOrder> orderRef) {
        if (selector == null) {
            return PropertyFactory.constSource(null);
        }

        return PropertyFactory.convert(installedPlatforms(), (JavaPlatform[] input) -> {
            return selector.selectPlatform(Arrays.asList(input), orderRef.getValue());
        });
    }

    private static PropertyValueDef<PlatformSelector, ScriptPlatform> getPlatformIdValueDef(
            final PropertySource<? extends PlatformOrder> orderRef) {
        Objects.requireNonNull(orderRef, "orderRef");

        return new PropertyValueDef<PlatformSelector, ScriptPlatform>() {
            @Override
            public PropertySource<ScriptPlatform> property(PlatformSelector valueKey) {
                return javaPlatform(valueKey, orderRef);
            }

            @Override
            public PlatformSelector getKeyFromValue(ScriptPlatform value) {
                if (value == null) {
                    return null;
                }

                switch (value.getSelectionMode()) {
                    case BY_VERSION:
                        return PlatformId.tryGetIdOfPlatform(value.getJavaPlatform());
                    case BY_LOCATION:
                        return new ExplicitPlatformRef(value.getJavaPlatform());
                    default:
                        throw new AssertionError(value.getSelectionMode().name());
                }
            }
        };
    }

    private ScriptPlatformProperty() {
        throw new AssertionError();
    }
}
