package org.netbeans.gradle.project.api.nodes;

import java.util.List;
import javax.annotation.Nonnull;
import javax.swing.Action;

/**
 * Defines a query which may return actions to be put in the context menu of the
 * project. That is, in the context menu when the user right clicks on the
 * project node.
 * <P>
 * Instances of this interface are expected to be found on the lookup of the extension
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * The actions in the context menu might may have their own visualization by
 * implementing the {@code Presenter.Popup} interface (e.g., they can have
 * submenus).
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently and might be called from the AWT Event Dispatch
 * Thread.
 *
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup()
 * @see GradleProjectAction
 */
public interface GradleProjectContextActions {
    /**
     * Returns the actions to be displayed in the context menu of the project.
     * The actions are displayed in the order they are returned.
     * <P>
     * This method is called every time, the context menu is requested to be
     * displayed.
     * <P>
     * You might annotate (not necessary) implementations of {@link Action}
     * with the {@link GradleProjectAction} annotation to give a hint to the
     * Gradle plugin in which group is your action is to be displayed.
     * <P>
     * The entries in the list can be {@code null} which will put a separator
     * line in the list. The {@link GradleProjectAction type} of the separator
     * is the same as the type of the previous non-null action in the list.
     * Separators cannot be placed in the beginning an at the end of the list,
     * so they are ignored at those locations.
     *
     * @return the actions to be displayed in the context menu of the project.
     *   This method may never return {@code null} but may return an empty list.
     */
    @Nonnull
    public List<Action> getContextActions();
}
