package org.netbeans.gradle.project.api.nodes;

import javax.annotation.Nullable;
import org.openide.nodes.Node;

/**
 * Defines a factory of a node to be displayed under the project node in the
 * project view.
 * <P>
 * The {@code createNode()} method of this interface are called from within
 * the {@code ChildFactory.createNodeForKey} method. It is undocumented in the
 * NetBeans API but is most likely called from the AWT Event Dispatch Thread.
 */
public interface SingleNodeFactory {
    /**
     * Creates a new node to be displayed under the project node. This method
     * must return a new node without a parent node added and must not return
     * an already cached node.
     *
     * @return a new node to be displayed under the project node. This method
     *   may return {@code null}, in which case no node is displayed.
     */
    @Nullable
    public Node createNode();
}
