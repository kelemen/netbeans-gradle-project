package org.netbeans.gradle.project.api.query;

/**
 * Defines a reference to a {@link ProjectPlatform}. This class is defined so
 * that the actual project platform can be created later (in case it might be
 * relatively expensive to create) but the available platform can still be shown
 * to the user.
 * <P>
 * Instances of this interface must be safe to be called by multiple threads
 * concurrently.
 *
 * @see GradleProjectPlatformQuery#getAvailablePlatforms()
 */
public interface ProjectPlatformRef {
    /**
     * Returns the user friendly name of the platform which might be displayed
     * to the user on a GUI.
     * <P>
     * This method should return the same string as
     * {@code getPlatform().getDisplayName()} (in terms of the {@code equals}
     * method).
     * <P>
     * This method must be cheap to be called and may be called from the
     * AWT Event Dispatch Thread. In fact, this method is recommended to only
     * return a {@code final} object field.
     *
     * @return the user friendly name of the platform which might be displayed
     *   to the user on a GUI. This method never returns {@code null}.
     */
    public String getDisplayName();

    /**
     * Returns the {@code ProjectPlatform} referenced by this
     * {@code ProjectPlatformRef}.
     * <P>
     * This method is allowed to do some I/O operations in order to gather
     * information about the platform. That is, this method may only called from
     * threads where blocking for a few seconds doesn't hurt the user experience
     * much. It is explicitly forbidden to call this method from the
     * AWT Event Dispatch Thread
     *
     * @return the {@code ProjectPlatform} referenced by this
     *   {@code ProjectPlatformRef}. This method never returns {@code null}.
     */
    public ProjectPlatform getPlatform();
}
