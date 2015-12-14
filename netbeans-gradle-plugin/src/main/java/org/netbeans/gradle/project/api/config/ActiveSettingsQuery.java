package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import org.jtrim.property.PropertySource;

/**
 * Defines the view of the project properties. An instance of this interface
 * can usually be requested through the {@link ProjectSettingsProvider ProjectSettingsProvider}
 * found on the {@link org.netbeans.api.project.Project#getLookup() project's lookup}.
 * <P>
 * The view of the project properties always assumes a selected profile.
 *
 * @see ProjectSettingsProvider
 */
public interface ActiveSettingsQuery {
    /**
     * Returns the property based on the given property definition. The value
     * of the property considers standard property value fallback rules
     * (e.g., falling back to default profile).
     * <P>
     * The returned property does not need to be re-requested. That is, calling
     * this method multiple times with the same argument returns effectively
     * the same property (note that this does not necessarily means reference equality).
     *
     * @param <ValueType> the type of the value of the property
     * @param propertyDef the definition of property defining how to parse or
     *   how to fallback to other values. This argument cannot be {@code null}.
     * @return the property based on the given definition. This method never
     *    returns {@code null}.
     */
    @Nonnull
    public <ValueType> PropertySource<ValueType> getProperty(@Nonnull PropertyDef<?, ValueType> propertyDef);

    /**
     * Returns the settings of the currently selected profile without any fallback
     * logic. That is, the values directly defined in that profile. Unlike the
     * {@link #getProperty(PropertyDef) getProperty} method, this lets you
     * change the values of a particular property in this profile.
     * <P>
     * If you need to explicitly specify a profile by {@link ProfileKey ProfileKey},
     * then you might want to use the method
     * {@link ProjectSettingsProvider.ExtensionSettings#loadSettingsForProfile(org.jtrim.cancel.CancellationToken, ProfileKey) ProjectSettingsProvider.ExtensionSettings.loadSettingsForProfile}
     * to load an {@code ActiveSettingsQuery} associated with a specific profile.
     * <P>
     * The current profile can usually only change if you are viewing the
     * settings actively in used by the user (i.e., the one which should be used
     * by actions executed by the user). In this case, the profile might be
     * changed by the user.
     *
     * @return the settings of the currently selected profile. This method never
     *   returns {@code null} and the value of the property is never {@code null}.
     */
    @Nonnull
    public PropertySource<SingleProfileSettings> currentProfileSettings();
}
