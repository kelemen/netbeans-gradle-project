package org.netbeans.gradle.project.api.entry;

import java.io.File;
import java.util.Map;
import javax.annotation.Nonnull;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the result of (re)loading the extension from models retrieved
 * from the evaluated build scripts.
 * <P>
 * The result may contain (and recommended to do) parsed models for multiple
 * projects (projects are identified by their project directory) which can be
 * saved for quicker initial load of the projects. These parsed models are
 * expected to be serializable.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * by multiple threads concurrently.
 *
 * @see GradleProjectExtension2#loadFromCache(Object) GradleProjectExtension2.loadFromCache
 * @see GradleProjectExtension2#loadFromModels(org.openide.util.Lookup)GradleProjectExtension2.loadFromModels
 */
public final class ExtensionLoadResult {
    private final boolean active;
    private final Map<File, Object> cachedModels;

    /**
     * Creates a new {@literal ExtensionLoadResult} with the given properties.
     *
     * @param active {@literal true} if the extension is to be considered active
     *   (enabled) for the particular project, {@literal false} if the extension
     *   is to be disabled
     * @param cachedModels the {@literal Map} mapping project directories to cached
     *   models. All models are expected to be serializable. This argument
     *   cannot be {@literal null} and cannot contain {@literal null} keys or values.
     */
    public ExtensionLoadResult(
            boolean active,
            @Nonnull Map<File, Object> cachedModels) {
        this.active = active;
        this.cachedModels = CollectionUtils.copyNullSafeHashMap(cachedModels);
    }

    /**
     * Returns {@literal true} if the associated extension is to be enabled for the
     * project.
     *
     * @return {@literal true} if the associated extension is to be enabled for the
     *   project, {@literal false} otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the models which can be used to quickly load the project without
     * evaluating the build scripts. The returned map maps project directories
     * (identifying a project) to models. All models are expected to be
     * serializable.
     * <P>
     * The returned models are passed to {@link GradleProjectExtension2#loadFromCache(Object)}
     * method.
     *
     * @return the {@literal Map} mapping project directories to cached models.
     *   This method never returns {@code null} and does not contain {@code null}
     *   keys or values.
     */
    @Nonnull
    public Map<File, Object> getCachedModels() {
        return cachedModels;
    }
}
