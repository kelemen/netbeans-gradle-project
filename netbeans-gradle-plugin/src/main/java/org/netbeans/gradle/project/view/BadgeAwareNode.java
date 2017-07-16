package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.beans.BeanInfo;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingProperties;
import org.jtrim2.property.swing.SwingPropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileStatusEvent;
import org.openide.filesystems.FileStatusListener;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUIUtils;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.ImageDecorator;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

import static org.jtrim2.property.PropertyFactory.*;

public final class BadgeAwareNode extends FilterNode {
    private static final Logger LOGGER = Logger.getLogger(BadgeAwareNode.class.getName());

    private final PropertySource<? extends Collection<File>> files;
    private final PropertySource<ImageDecorator> statusProperty;

    private final UpdateTaskExecutor fileUpdater;
    private final UpdateTaskExecutor iconChangeNotifier;
    private final MutableProperty<FileObjects> fileObjs;
    private final ListenerRegistrations listenerRegs;

    private BadgeAwareNode(Node original, PropertySource<? extends Collection<File>> files) {
        super(original);
        Objects.requireNonNull(files, "files");

        this.files = files;
        this.listenerRegs = new ListenerRegistrations();
        this.fileObjs = lazilySetProperty(memProperty(new FileObjects()));
        this.fileUpdater = NbTaskExecutors.newDefaultUpdateExecutor();
        this.iconChangeNotifier = SwingExecutors.getSwingUpdateExecutor(true);
        this.statusProperty = PropertyFactory.propertyOfProperty(fileObjs, arg -> new FileSystemStatusProperty(arg).toStandard());
    }

    public static Node makeBadgeAware(Node original, PropertySource<? extends Collection<File>> files) {
        BadgeAwareNode result = new BadgeAwareNode(original, files);
        result.init();
        return result;
    }

    private static FileSystem getFileSystem(Set<FileObject> fileObjs) {
        if (fileObjs.isEmpty()) {
            return null;
        }

        for (FileObject fileObj: fileObjs) {
            try {
                return fileObj.getFileSystem();
            } catch (FileStateInvalidException ex) {
                LOGGER.log(Level.INFO, "FileSystem is unavailable for file: " + fileObj, ex);
            }
        }
        return null;
    }

    private void updateFilesNow(Collection<File> currentFiles) {
        fileObjs.setValue(new FileObjects(currentFiles));
    }

    private void updateFiles() {
        fileUpdater.execute(() -> {
            Collection<File> currentFiles = files.getValue();
            updateFilesNow(currentFiles != null ? currentFiles : Collections.<File>emptySet());
        });
    }

    private void updateIcons() {
        iconChangeNotifier.execute(this::updateIconsNow);
    }

    private void updateIconsNow() {
        fireIconChange();
        fireOpenedIconChange();
    }

    public void init() {
        listenerRegs.add(NbProperties.weakListenerProperty(files).addChangeListener(this::updateFiles));
        updateFiles();
        listenerRegs.add(NbProperties.weakListenerProperty(statusProperty).addChangeListener(this::updateIcons));
    }

    private Image annotate(Image src) {
        ImageDecorator status = statusProperty.getValue();
        return status != null ? status.annotateIcon(src, BeanInfo.ICON_COLOR_16x16, fileObjs.getValue().fileObjs) : src;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return annotate(super.getOpenedIcon(type));
    }

    @Override
    public Image getIcon(int type) {
        return annotate(super.getIcon(type));
    }

    @Override
    public void destroy() throws IOException {
        listenerRegs.unregisterAll();

        super.destroy();
    }

    private static final class FileObjects {
        private final Set<FileObject> fileObjs;
        private final FileSystem fileSystem;

        public FileObjects() {
            this(Collections.<File>emptySet());
        }

        public FileObjects(Collection<File> files) {
            Set<FileObject> newFileObjs = CollectionUtils.newHashSet(files.size());
            for (File file: files) {
                FileObject fileObj = file != null ? FileUtil.toFileObject(file) : null;
                if (fileObj != null) {
                    newFileObjs.add(fileObj);
                }
            }

            this.fileObjs = Collections.unmodifiableSet(newFileObjs);
            this.fileSystem = getFileSystem(newFileObjs);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.fileObjs);
            hash = 97 * hash + System.identityHashCode(this.fileSystem);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final FileObjects other = (FileObjects)obj;
            return this.fileSystem == other.fileSystem
                    && Objects.equals(this.fileObjs, other.fileObjs);
        }
    }

    private static final class FileSystemStatusProperty implements SwingPropertySource<ImageDecorator, FileStatusListener> {
        private final FileObjects fileObjects;

        public FileSystemStatusProperty(FileObjects fileObjects) {
            assert fileObjects != null;
            this.fileObjects = fileObjects;
        }

        public PropertySource<ImageDecorator> toStandard() {
            return SwingProperties.fromSwingSource(this, (Runnable listener) -> {
                Objects.requireNonNull(listener, "listener");

                return (FileStatusEvent ev) -> {
                    if (anythingChanged(ev)) {
                        listener.run();
                    }
                };
            });
        }

        private boolean anythingChanged(FileStatusEvent ev) {
            for (FileObject fileObj: fileObjects.fileObjs) {
                if (ev.hasChanged(fileObj)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public ImageDecorator getValue() {
            FileSystem fs = fileObjects.fileSystem;
            return FileUIUtils.getImageDecorator(fs);
        }

        @Override
        public void addChangeListener(FileStatusListener listener) {
            FileSystem fs = fileObjects.fileSystem;
            if (fs != null) {
                fs.addFileStatusListener(listener);
            }
        }

        @Override
        public void removeChangeListener(FileStatusListener listener) {
            FileSystem fs = fileObjects.fileSystem;
            if (fs != null) {
                fs.removeFileStatusListener(listener);
            }
        }
    }
}
