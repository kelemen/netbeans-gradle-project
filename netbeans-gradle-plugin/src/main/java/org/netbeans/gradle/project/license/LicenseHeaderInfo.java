package org.netbeans.gradle.project.license;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the license and associated properties what can be added to
 * newly created files.
 * <P>
 * Instances of this class are immutable.
 */
public final class LicenseHeaderInfo {
    private final String licenseName;
    private final Map<String, String> properties;
    private final Path licenseTemplateFile;

    /**
     * Creates the license information with the given properties.
     *
     * @param licenseName the name of the license with which users can
     *   reference the license. This argument cannot be {@code null}.
     * @param properties variables and their values to replace references
     *   to them in the license template. This argument cannot be {@code null},
     *   and cannot contain {@code null} elements.
     * @param licenseTemplateFile the path to the license file. This path can
     *   be relative, in which case it is resolved against the root project
     *   directory. This argument can be {@code null}, if the IDE is expected
     *   to have a license with the given name.
     */
    public LicenseHeaderInfo(
            @Nonnull String licenseName,
            @Nonnull Map<String, String> properties,
            @Nullable Path licenseTemplateFile) {
        this.licenseName = Objects.requireNonNull(licenseName, "licenseName");
        this.properties = CollectionUtils.copyNullSafeHashMap(properties);
        this.licenseTemplateFile = licenseTemplateFile;
    }

    /**
     * Returns the file containing the license. The license might contain
     * variables to be replaced. The values of these variables are defined by
     * the {@link #getProperties() getProperties()} method.
     * <P>
     * The returned path can be relative, in which case the path is resolved
     * against the root path of the project where the license is applied.
     * <P>
     * Having a license file is optional. If there is no license file,
     * the license is solely identified by its {@link #getLicenseName() name}
     * and it is assumed that the IDE can recognize the license by its name.
     *
     * @return the file containing the license or {@code null} if the license
     *   is identified by its name only
     */
    @Nullable
    public Path getLicenseTemplateFile() {
        return licenseTemplateFile;
    }

    /**
     * Returns the name of the license as specified by the user. The name
     * must be something that is meaningful to the user. If there is no
     * {@link #getLicenseTemplateFile() license template file}, the license
     * is solely identified by its name.
     *
     * @return the name of the license as specified by the user. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public String getLicenseName() {
        return licenseName;
    }

    /**
     * Returns the values (and names) of the variables to be replaced in the
     * license template. The keys of the returned maps are the names of the
     * variables and their associated value is the value.
     *
     * @return the values (and names) of the variables to be replaced in the
     *   license template. This method never returns {@code null} and does not
     *   contain {@code null} elements.
     */
    @Nonnull
    public Map<String, String> getProperties() {
        return properties;
    }
}
