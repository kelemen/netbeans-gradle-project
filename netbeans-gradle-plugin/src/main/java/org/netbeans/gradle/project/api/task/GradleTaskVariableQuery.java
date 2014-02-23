package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.openide.util.Lookup;

/**
 * Defines a query which is able to retrieve variable mapping used to replace
 * strings when executing Gradle tasks. Variables might be used in task names,
 * task arguments or JVM arguments of the executed Gradle tasks. Variables in
 * strings take the form "${variable-name}". For example a built-in variable is
 * "project", so writing "${project}:run" will be replaced with ":MyProject:run",
 * assuming that the name of the project is ":MyProject".
 * <P>
 * Instances of this interface are expected to be found on the lookup of the extension
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * The actions in the context menu might may have their own visualization by
 * implementing the {@code Presenter.Popup} interface (e.g., they can have
 * submenus).
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup()
 */
public interface GradleTaskVariableQuery {
    /**
     * Returns the map returning the replacement string for certain variables.
     * <P>
     * This method is always called from a background thread just before each
     * Gradle command. Therefore, this method may do some I/O operation. Note,
     * however, that this method should return relatively quickly, otherwise
     * each Gradle command execution will suffer a performance penalty (due to
     * this method is executed prior the Gradle command).
     * <P>
     * Note that {@code Lookup} will always contain an instance of
     * {@link NbCommandString} which specifies the command string passed to the
     * {@link org.netbeans.spi.project.ActionProvider ActionProvider} implementation.
     *
     * @param actionContext the lookup provided by NetBeans for this task
     *   execution. If the command being executed is a built-in command, then
     *   this lookup is the same as passed in the
     *   {@code ActionProvider.invokeAction} method. This argument cannot be
     *   {@code null} but might be possibly empty.
     * @return the map mapping the names of variables to their values. The
     *   returned map can be (and recommended to be) lazily constructed. This
     *   method may never return {@code null}.
     *
     * @see NbCommandString
     */
    @Nonnull
    public TaskVariableMap getVariableMap(@Nonnull Lookup actionContext);
}
