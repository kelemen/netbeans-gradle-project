package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;

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

    @Nonnull
    public MutableProperty<String> getSourceLevel();

    @Nonnull
    public MutableProperty<ProjectPlatform> getPlatform();

    @Nonnull
    public MutableProperty<JavaPlatform> getScriptPlatform();

    @Nonnull
    public MutableProperty<GradleLocation> getGradleLocation();

    @Nonnull
    public MutableProperty<Charset> getSourceEncoding();

    @Nonnull
    public MutableProperty<List<PredefinedTask>> getCommonTasks();

    @Nonnull
    public MutableProperty<LicenseHeaderInfo> getLicenseHeader();

    @Nullable
    public MutableProperty<PredefinedTask> tryGetBuiltInTask(String command);

    @Nonnull
    public Set<String> getKnownBuiltInCommands();

    // The listener of this property is always notified whenever any of the
    // auxilary configuration changes.
    @Nonnull
    public MutableProperty<Void> getAuxConfigListener();

    @Nonnull
    public AuxConfigProperty getAuxConfig(@Nullable String elementName, @Nullable String namespace);

    @Nonnull
    public void setAllAuxConfigs(@Nonnull Collection<AuxConfig> configs);

    @Nonnull
    public Collection<AuxConfigProperty> getAllAuxConfigs();

    @Nonnull
    public Collection<MutableProperty<?>> getAllProperties();
}
