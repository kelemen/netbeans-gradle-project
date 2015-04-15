package org.netbeans.gradle.project.api.nodes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.openide.nodes.Node;

/**
 * Looks for an {@link Node} identified by the given object. This interface
 * is expected to be found on the {@link Node#getLookup() lookup} of
 * {@code Node} instances by the {@code LogicalViewProvider} provided by the
 * project. That is, if object implementing this interface is found on the
 * lookup of a node, then no other means to find the requested object is done
 * but the one provided by the {@code NodeFinder}.
 * <P>
 * There can be more than one instances of this interface on the lookup. In this
 * case all {@code NodeFinder} instances are asked to find the node until one
 * of them finds a node (or noone finds one).
 *
 * @see org.netbeans.spi.project.ui.LogicalViewProvider
 * @see org.netbeans.spi.project.ui.PathFinder
 */
public interface NodeFinder {
    /**
     * Returns the {@code Node} associated with the given object or {@code null}
     * if there is no such node. If the implementation does not recognizes
     * the object, it must return {@code null}. That is, it must expect object
     * of any class as the target object.
     *
     * @param target the object identifing the node to be found. This argument
     *   cannot be {@code null}. This argument is usually (but not necessarily)
     *   is a {@link org.openide.filesystems.FileObject FileObject}.
     * @return the {@code Node} associated with the given object or {@code null}
     *   if there is no such node
     */
    @Nullable
    public Node findNode(@Nonnull Object target);
}
