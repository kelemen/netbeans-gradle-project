package org.netbeans.gradle.project.filesupport;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.text.DataEditorSupport;

@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_GradleFile",
        mimeType = "text/x-gradle",
        extension = {"gradle", "Gradle", "GRADLE"})
@DataObject.Registration(
        mimeType = "text/x-gradle",
        iconBase = "org/netbeans/gradle/project/resources/gradle.png",
        displayName = "#LBL_GradleFile",
        position = 300)
@SuppressWarnings("serial")
public final class GradleDataObject extends MultiDataObject {
    public GradleDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("text/x-gradle", false);
        getLookup().lookup(DataEditorSupport.class).setMIMEType("text/x-groovy");
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Override
    protected Node createNodeDelegate() {
        return new DataNode(this, Children.LEAF, getLookup());
    }
}
