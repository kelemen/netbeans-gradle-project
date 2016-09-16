package org.netbeans.gradle.project.api.config.ui;

/**
 * @deprecated Use the {@link ProfileBasedSettingsPageFactory} based configuration instead:
 *   {@link ProfileEditor}.
 * <P>
 * Defines the logic of editing and saving properties of an associated profile.
 *
 * @see ProfileBasedConfigurations
 * @see ProfileValuesEditorFactory
 */
@Deprecated
public interface ProfileValuesEditor {
    /**
     * Displays the last value of the properties to be edited on the associated
     * settings component. That is, displays the values stored in the previous
     * {@link #readFromGui() readFromGui()} method; or if the {@code readFromGui}
     * method was not called yet, the current values of the properties to be
     * edited.
     * <P>
     * This method is always called on the <I>Event Dispatch Thread</I>.
     */
    public void displayValues();

    /**
     * Reads and remebers the values entered on the property editor component.
     * Note that this method is not supposed to adjust the values of the edited
     * properties. This method is only used to prepare these values for the
     * {@link #displayValues() displayValues} and the
     * {@link #applyValues() applyValues} methods. If this method is called
     * multiple times, only the values read by the last call to this method
     * must be remembered, previously read values can (and should) be discarded.
     * <P>
     * This method is always called on the <I>Event Dispatch Thread</I>.
     */
    public void readFromGui();

    /**
     * Adjusts the values of the edited property to the values read by the last
     * {@link #readFromGui() readFromGui} method call. Note that this method
     * must ignore everything currently on the associated editor component and
     * only use the values saved by the last {@code readFromGui} call.
     * <P>
     * Note that this method can never be called concurrently with other method
     * calls of this interface. Also, this method may only gets called after the
     * {@code readFromGui} method has been called at least once.
     */
    public void applyValues();
}
