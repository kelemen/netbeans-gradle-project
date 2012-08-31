package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collection;
import org.netbeans.api.java.platform.JavaPlatform;

public interface ProjectProperties {
    // When adding new properties, don't forget to update:
    //   - PropertiesSnapshot: Add a field for the new property.
    //   - XmlPropertiesPersister.load: Do the same as with other properties.
    //   - AbstractProjectProperties.getAllProperties: Return the new property as well.
    //   - PropertiesSnapshot's public constructor: Check the newly added property as well
    //
    //   Other places need to be updated will not compile.

    public MutableProperty<String> getSourceLevel();
    public MutableProperty<JavaPlatform> getPlatform();
    public MutableProperty<Charset> getSourceEncoding();

    public Collection<MutableProperty<?>> getAllProperties();
}
