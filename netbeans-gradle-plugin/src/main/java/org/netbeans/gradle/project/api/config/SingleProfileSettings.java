package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim2.property.MutableProperty;

/**
 * Defines the settings of particular profile without any value fallback rules.
 * <P>
 * Usually, you can get an instance of this interface by requesting an
 * {@link ActiveSettingsQuery} through a {@link ProjectSettingsProvider}.
 *
 * @see ActiveSettingsQuery
 * @see ProjectSettingsProvider
 */
public interface SingleProfileSettings {
    /**
     * Returns the {@link ProfileKey key} identifying this profile within the
     * owner project.
     *
     * @return the {@link ProfileKey key} identifying this profile within the
     *   owner project. This method may return {@code null} if this profile is
     *   the default profile.
     */
    @Nullable
    public ProfileKey getKey();

    /**
     * Returns the property storing the value of the specified project property.
     * <P>
     * If this profile has no definition of the given property, the value of the
     * property will be {@code null}. That is, there won't be any fallback rules
     * applied.
     * <P>
     * Setting the value of the property will eventually save it to a persistent
     * location (i.e., a project properties file). Note however, that regardless
     * when the property actually gets saved to disk, you will always properly
     * see the appropriate (the value last set) value even if you re-request this
     * property or even this profile.
     * <P>
     * The returned property does not need to be re-requested. That is, calling
     * this method multiple times with the same argument returns effectively
     * the same property (note that this does not necessarily means reference equality).
     *
     * @param <ValueType> the type of the requested property
     * @param propertyDef the {@code PropertyDef} defining the requested
     *   property. This argument cannot be {@code null}.
     * @return the property storing the value of the specified project property.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public <ValueType> MutableProperty<ValueType> getProperty(@Nonnull PropertyDef<?, ValueType> propertyDef);
}
