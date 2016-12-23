package org.netbeans.gradle.project.filesupport;

import java.io.IOException;
import org.netbeans.core.api.multiview.MultiViews;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.GroovyScripts;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.EditCookie;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.PrintCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.DataEditorSupport;
import org.openide.windows.CloneableOpenSupport;

@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_GradleFile",
        mimeType = GradleDataObject.GRADLE_MIME_TYPE,
        extension = {"gradle", "Gradle", "GRADLE"})
@DataObject.Registration(
        mimeType = "text/x-gradle",
        iconBase = NbIcons.PROJECT_ICON_PATH,
        displayName = "#LBL_GradleFile",
        position = 300)
@ActionReferences({
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
        position = 100,
        separatorAfter = 200),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id =@ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
        position = 300),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
        position = 400,
        separatorAfter = 500),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
        position = 600),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
        position = 700,
        separatorAfter = 800),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
        position = 900,
        separatorAfter = 1000),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
        position = 1100,
        separatorAfter = 1200),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
        position = 1300),
    @ActionReference(
        path = "Loaders/text/x-gradle/Actions",
        id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
        position = 1400)
})
public final class GradleDataObject extends MultiDataObject {
    private static final long serialVersionUID = 814372868086075839L;

    public static final String GRADLE_MIME_TYPE = "text/x-gradle";
    private static final String GROOVY_MIME_TYPE = "text/x-groovy";

    private static final String BUILD_FILE_NAME = CommonScripts.BUILD_BASE_NAME + GroovyScripts.EXTENSION;
    private static final String SETTINGS_FILE_NAME = CommonScripts.SETTINGS_BASE_NAME + GroovyScripts.EXTENSION;

    public GradleDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException {
        super(pf, loader);

        getCookieSet().add(new GradleDataEditor());
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Override
    protected Node createNodeDelegate() {
        return new DataNode(this, Children.LEAF, getLookup());
    }

    private class GradleDataEditor
    extends
            DataEditorSupport
    implements
            EditorCookie.Observable,
            OpenCookie,
            EditCookie,
            PrintCookie,
            CloseCookie {

        private final SaveCookie save;
        private final FileChangeListener listener;

        public GradleDataEditor() {
            super(GradleDataObject.this, null, new GradleEnv(GradleDataObject.this));

            save = new SaveCookie() {
                @Override
                public void save() throws IOException {
                    saveDocument();
                }

                @Override
                public String toString() {
                    return getPrimaryFile().getNameExt();
                }
            };
            listener = new FileChangeAdapter() {
                @Override
                public void fileChanged(FileEvent fe) {
                    updateTitles();
                }
            };

            getPrimaryFile().addFileChangeListener(FileUtil.weakFileChangeListener(listener, getPrimaryFile()));
            setMIMEType(GROOVY_MIME_TYPE);
        }

        @Override
        protected Pane createPane() {
            return (CloneableEditorSupport.Pane)MultiViews.createCloneableMultiView(GROOVY_MIME_TYPE, getDataObject());
        }

        @Override
        protected boolean notifyModified() {
            if (!super.notifyModified()) {
                return false;
            }
            if (getLookup().lookup(SaveCookie.class) == null) {
                getCookieSet().add(save);
                setModified(true);
            }
            return true;
        }

        @Override
        protected void notifyUnmodified() {
            super.notifyUnmodified();
            if (getLookup().lookup(SaveCookie.class) == save) {
                getCookieSet().remove(save);
                setModified(false);
            }
        }

        @Override
        protected String messageName() {
            return annotateWithFolder(super.messageName());
        }

        @Override
        protected String messageHtmlName() {
            String name = super.messageHtmlName();
            return name != null ? annotateWithFolder(name) : null;
        }

        private boolean shouldAnnotate(String baseFileName) {
            return BUILD_FILE_NAME.equalsIgnoreCase(baseFileName)
                    || SETTINGS_FILE_NAME.equalsIgnoreCase(baseFileName);
        }

        private String annotateWithFolder(String name) {
            if (shouldAnnotate(getPrimaryFile().getNameExt())) {
                FileObject parent = getPrimaryFile().getParent();
                if (parent != null) {
                    String folderName = parent.getNameExt();
                    return name + " [" + folderName + "]";
                }
            }

            return name;
        }

        @Override
        protected boolean asynchronousOpen() {
            return true;
        }
    }

    private static class GradleEnv extends DataEditorSupport.Env {
        private static final long serialVersionUID = 136529845402150749L;

        public GradleEnv(MultiDataObject dataObject) {
            super(dataObject);
        }

        @Override
        protected FileObject getFile() {
            return getDataObject().getPrimaryFile();
        }

        @Override
        protected FileLock takeLock() throws IOException {
            return ((MultiDataObject)getDataObject()).getPrimaryEntry().takeLock();
        }

        @Override
        public CloneableOpenSupport findCloneableOpenSupport() {
            return getDataObject().getLookup().lookup(GradleDataEditor.class);
        }
    }
}
