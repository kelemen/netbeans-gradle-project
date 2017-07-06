package org.netbeans.gradle.project.api.config.ui;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.swing.JComponent;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;

/**
 * Defines a profile based property editor page. Instances of these
 * classes are usually created by a {@link ProfileBasedSettingsPageFactory}.
 *
 * @see ProfileBasedSettingsCategory
 * @see ProfileBasedSettingsPageFactory
 */
public class ProfileBasedSettingsPage {
    private final JComponent settingsPanel;
    private final ProfileEditorFactory editorFactory;
    private final CancelableFunction<? extends Runnable> asyncPanelInitializer;

    /**
     * Creates a new {@code ProfileBasedSettingsPageFactory} with the
     * given <I>Swing</I> component and the logic of saving and updating
     * properties on this component.
     *
     * @param settingsPanel the <I>Swing</I> component displaying the editors
     *   of the properties to be adjusted. This argument cannot be {@code null}.
     * @param editorFactory the logic of saving and updating properties edited
     *   on this page. This argument cannot be {@code null}.
     */
    public ProfileBasedSettingsPage(@Nonnull JComponent settingsPanel, @Nonnull ProfileEditorFactory editorFactory) {
        this(settingsPanel, editorFactory, NoOpAsyncTask.NO_OP);
    }

    /**
     * Creates a new {@code ProfileBasedSettingsPageFactory} with the
     * given <I>Swing</I> component and the logic of saving and updating
     * properties on this component.
     *
     * @param settingsPanel the <I>Swing</I> component displaying the editors
     *   of the properties to be adjusted. This argument cannot be {@code null}.
     * @param editorFactory the logic of saving and updating properties edited
     *   on this page. This argument cannot be {@code null}.
     * @param asyncPanelInitializer a task which is used to initialize the panel before
     *   it becomes editable. This argument cannot be {@code null}.
     *   See {@link #getAsyncPanelInitializer() getAsyncPanelInitializer()} for further details.
     */
    public ProfileBasedSettingsPage(
            @Nonnull JComponent settingsPanel,
            @Nonnull ProfileEditorFactory editorFactory,
            @Nonnull CancelableFunction<? extends Runnable> asyncPanelInitializer) {
        this.settingsPanel = Objects.requireNonNull(settingsPanel, "settingsPanel");
        this.editorFactory = Objects.requireNonNull(editorFactory, "editorFactory");
        this.asyncPanelInitializer = Objects.requireNonNull(asyncPanelInitializer, "asyncPanelInitializer");
    }

    /**
     * Returns the <I>Swing</I> component displaying the editors of the
     * properties to be adjusted.
     *
     * @return the <I>Swing</I> component displaying the editors of the
     *   properties to be adjusted. This method never returns {@code null}.
     */
    @Nonnull
    public final JComponent getSettingsPanel() {
        return settingsPanel;
    }

    /**
     * Returns the logic of saving and updating properties edited on this page.
     *
     * @return the logic of saving and updating properties edited on this page.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public final ProfileEditorFactory getEditorFactory() {
        return editorFactory;
    }

    /**
     * Returns a task which is used to initialize the associated settings panel. The initialization
     * is done the following way:
     * <ol>
     *  <li>
     *   The task is run on a background thread (never on the Event Dispatch Thread).
     *  </li>
     *  <li>
     *   The returned {@code Runnable} is executed on the Event Dispatch Thread.
     *  </li>
     *  <li>
     *   After the returned {@code Runnable} completes, the panel is made editable
     *   to the user.
     *  </li>
     * </ol>
     * The asynchronous task may return {@code null} if it does not want to update
     * the UI.
     *
     * @return a task which is used to initialize the associated settings panel. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public CancelableFunction<? extends Runnable> getAsyncPanelInitializer() {
        return asyncPanelInitializer;
    }

    private enum NoOpAsyncTask implements CancelableFunction<Runnable> {
        NO_OP;

        @Override
        public Runnable execute(CancellationToken cancelToken) {
            return null;
        }
    }
}
