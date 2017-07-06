package org.netbeans.gradle.project.properties.standard;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.util.StringUtils;

public final class SourceEncodingProperty {
    private static final Logger LOGGER = Logger.getLogger(SourceEncodingProperty.class.getName());

    public static final Charset DEFAULT_SOURCE_ENCODING = StringUtils.UTF8;

    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("source-encoding");

    public static final PropertyDef<String, Charset> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, Charset> createPropertyDef() {
        PropertyDef.Builder<String, Charset> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);

        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(getValueDef());
        return result.create();
    }

    private static PropertyValueDef<String, Charset> getValueDef() {
        return new PropertyValueDef<String, Charset>() {
            @Override
            public PropertySource<Charset> property(String valueKey) {
                Charset charset;
                try {
                    charset = valueKey != null ? Charset.forName(valueKey) : null;
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.WARNING, "Illegal charset name: " + valueKey, ex);
                    charset = null;
                }

                return PropertyFactory.constSource(charset);
            }

            @Override
            public String getKeyFromValue(Charset value) {
                return value != null ? value.name() : null;
            }
        };
    }

    private SourceEncodingProperty() {
        throw new AssertionError();
    }
}
