package org.netbeans.gradle.project.properties2.standard;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyValueDef;

public final class SourceEncodingProperty {
    private static final Logger LOGGER = Logger.getLogger(SourceEncodingProperty.class.getName());

    private static final PropertyDef<String, Charset> PROPERTY_DEF = createPropertyDef();
    private static final String CONFIG_KEY_SOURCE_ENCODING = "source-encoding";

    public static PropertySource<Charset> getProperty(ProjectProfileSettings settings) {
        List<ConfigPath> paths = Arrays.asList(ConfigPath.fromKeys(CONFIG_KEY_SOURCE_ENCODING));
        return settings.getProperty(paths, getPropertyDef());
    }

    public static PropertyDef<String, Charset> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static PropertyDef<String, Charset> createPropertyDef() {
        PropertyDef.Builder<String, Charset> result = new PropertyDef.Builder<>();
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
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

    private SourceEncodingProperty() {
        throw new AssertionError();
    }
}
