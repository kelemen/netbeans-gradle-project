package org.netbeans.gradle.project.model;

/**
 * Objects of this type on the project's lookup will be automatically
 * notified whenever the underlying {@link NbGradleProject#currentModel() model}
 * changes.
 */
public interface ProjectModelChangeListener {
    /**
     * Called after the underlying model of the owner project changes.
     * This method is called after all changes have taken place (including the
     * models of the extensions).
     * <P>
     * This method is always called on the Event Dispatch Thread.
     */
    public void onModelChanged();
}
