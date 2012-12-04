package org.netbeans.gradle.project.properties;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import org.netbeans.api.java.platform.JavaPlatform;

public interface ProjectProperties {
    // Methods of this interface must be safe to access from multiple threads
    // concurrently.

    // When adding new properties, don't forget to update:
    //   - PropertiesSnapshot: Add a field for the new property.
    //   - XmlPropertiesPersister.load: Do the same as with other properties.
    //   - AbstractProjectProperties.getAllProperties: Return the new property as well.
    //   - PropertiesSnapshot's public constructor: Check the newly added property as well
    //   - Implement saving and loading in XmlPropertyFormat
    //
    //   Other places need to be updated will not compile.

    public MutableProperty<String> getSourceLevel();
    public MutableProperty<JavaPlatform> getPlatform();
    public MutableProperty<JavaPlatform> getScriptPlatform();
    public MutableProperty<File> getGradleHome();
    public MutableProperty<Charset> getSourceEncoding();
    public MutableProperty<List<PredefinedTask>> getCommonTasks();

    // In case you add more built-in tasks, don't forget to update
    // AbstractProjectProperties.getCustomizableCommands().
    // Also define the defaults in BuiltInTasks.
    public MutableProperty<PredefinedTask> tryGetBuiltInTask(String command);

    public Collection<MutableProperty<?>> getAllProperties();
}
