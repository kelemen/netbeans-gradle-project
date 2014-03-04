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
    public OldMutableProperty<String> getSourceLevel();

    @Nonnull
    public OldMutableProperty<ProjectPlatform> getPlatform();

    @Nonnull
    public OldMutableProperty<JavaPlatform> getScriptPlatform();

    @Nonnull
    public OldMutableProperty<GradleLocation> getGradleLocation();

    @Nonnull
    public OldMutableProperty<Charset> getSourceEncoding();

    @Nonnull
    public OldMutableProperty<List<PredefinedTask>> getCommonTasks();

    @Nonnull
    public OldMutableProperty<LicenseHeaderInfo> getLicenseHeader();

    @Nullable
    public OldMutableProperty<PredefinedTask> tryGetBuiltInTask(String command);

    @Nonnull
    public Set<String> getKnownBuiltInCommands();

    // The listener of this property is always notified whenever any of the
    // auxilary configuration changes.
    @Nonnull
    public OldMutableProperty<Void> getAuxConfigListener();

    @Nonnull
    public AuxConfigProperty getAuxConfig(@Nullable String elementName, @Nullable String namespace);

    @Nonnull
    public void setAllAuxConfigs(@Nonnull Collection<AuxConfig> configs);

    @Nonnull
    public Collection<AuxConfigProperty> getAllAuxConfigs();

    @Nonnull
    public Collection<OldMutableProperty<?>> getAllProperties();
}
