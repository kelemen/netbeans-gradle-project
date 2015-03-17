package org.netbeans.gradle.project.properties.standard;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties.ConfigPath;
import org.netbeans.gradle.project.properties.PropertyDef;
import org.netbeans.gradle.project.properties.PropertyValueDef;

public final class SourceEncodingProperty {
    private static final Logger LOGGER = Logger.getLogger(SourceEncodingProperty.class.getName());

    public static final Charset DEFAULT_SOURCE_ENCODING = Charset.forName("UTF-8");

    private static final String CONFIG_KEY_SOURCE_ENCODING = "source-encoding";

    public static final PropertyDef<String, Charset> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, Charset> createPropertyDef() {
        PropertyDef.Builder<String, Charset> result
                = new PropertyDef.Builder<>(ConfigPath.fromKeys(CONFIG_KEY_SOURCE_ENCODING));

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
