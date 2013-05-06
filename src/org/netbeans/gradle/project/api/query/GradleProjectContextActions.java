package org.netbeans.gradle.project.api.query;

import java.util.List;
import javax.swing.Action;

/**
 * Defines a query which may return actions to be put in the context menu of the
 * project. That is, in the context menu when the user right clicks on the
 * project node.
 * <P>
 * Instances of this interface are expected to be found on the
 * {@link GradleProjectExtension#getExtensionLookup() lookup of the extension}.
 * The actions in the context menu might may have their own visualization by
 * implementing the {@code Presenter.Popup} interface (e.g., they can have
 * submenus).
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently and might be called from the AWT Event Dispatch
 * Thread.
 *
 * @see GradleProjectExtension#getExtensionLookup()
 *
 * @author Kelemen Attila
 */
public interface GradleProjectContextActions {
    /**
     * Returns the actions to be displayed in the context menu of the project.
     * The actions are displayed in the order they are returned.
     * <P>
     * This method is called every time, the context menu is requested to be
     * displayed.
     *
     * @return the actions to be displayed in the context menu of the project.
     *   This method may never return {@code null} but may return an empty list.
     */
    public List<Action> getContextActions();
}
