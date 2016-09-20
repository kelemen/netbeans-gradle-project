package org.netbeans.gradle.project.java.nodes;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.util.WeakListeners;

public class FileListenerHandler {

    private static final Logger LOGGER = Logger.getLogger(FileListenerHandler.class.getName());
    private final ChangeListenerManager changedListener = new GenericChangeListenerManager(GenericChangeListenerManager.getSwingNotifier());
    private final FileChangeListener fileChangeListener = new FileChangeAdapter() {
        @Override
        public void fileDeleted(final FileEvent fe) {
            LOGGER.log(Level.INFO, "File {0} has been deleted.", fe.getFile().getPath());

            if (modelChangingPaths.contains(fe.getFile().getPath())) {
                fireModelChange();
            }
        }

        @Override
        public void fileFolderCreated(final FileEvent fe) {
            LOGGER.log(Level.INFO, "Folder {0} has been created.", new Object[]{fe.getFile().getPath()});

            if (modelChangingPaths.contains(fe.getFile().getPath())) {
                fireModelChange();
            }
        }

        @Override
        public void fileRenamed(final FileRenameEvent fe) {
            if (null == fe.getExt() || fe.getExt().isEmpty()) {
                LOGGER.log(Level.INFO, "Folder {0} has been renamed.", fe.getName());
            } else {
                LOGGER.log(Level.INFO, "File {0}.{1} has been renamed.", new Object[]{fe.getName(), fe.getExt()});
            }

            if (modelChangingPaths.contains(fe.getFile().getPath())) {
                fireModelChange();
            }
        }
    };
    private final JavaExtension javaExt;
    private final Collection<String> modelChangingPaths = new HashSet<>();

    public FileListenerHandler(final JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");
        this.javaExt = javaExt;
    }

    /**
     * Registers a listener to be notified when a FileSystemChange is done that
     * will result in a refresh of the project nodes.
     * <P>
     * The listeners might be notified on any thread.
     *
     * @param listener the listener whose {@code run} method is to be called
     *   whenever a change occurs. This argument cannot be {@code null}.
     * @return the {@code ListenerRef} which can be used to unregister the
     *   currently added listener, so that it may not be notified again. This
     *   method never returns {@code null}.
     */
    public NbListenerRef addChangeListener(final Runnable listener) {
        ListenerRef ref = changedListener.registerListener(listener);

        modelChangingPaths.clear();

        for (final NamedSourceRoot namedSourceRoot : javaExt.getCurrentModel().getMainModule().getNamedSourceRoots()) {
            modelChangingPaths.add(namedSourceRoot.getRoot().getAbsolutePath());
        }
        
        LOGGER.log(Level.WARNING, "Listening to these folders: {0}", modelChangingPaths);
        FileObject fileObject = javaExt.getProject().getProjectDirectory();

        if (null == fileObject) {
            LOGGER.log(Level.WARNING, "Unable to find FileObject for '{0}'", javaExt.getProjectDirectoryAsFile());
        } else {
            fileObject.addRecursiveListener(WeakListeners.create(FileChangeListener.class, fileChangeListener, fileObject));
        }

        return NbListenerRefs.asNbRef(ref);
    }

    private void fireModelChange() {
        changedListener.fireEventually();
    }
}
