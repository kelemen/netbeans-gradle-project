package org.netbeans.gradle.project.api.nodes;

/**
 * Defines an interface which allows to refresh the child nodes of an
 * associated {@link org.openide.nodes.Node Node}. Instances of this interface
 * are expected to be found on the {@code Node}'s lookup.
 */
public interface NodeRefresher {
    /**
     * Refreshes the child nodes of the associated node. That is, looks
     * for external undetected changes as if the node had just been created.
     * <P>
     * This method is always called from Event Dispatch Thread.
     */
    public void refreshNode();
}
