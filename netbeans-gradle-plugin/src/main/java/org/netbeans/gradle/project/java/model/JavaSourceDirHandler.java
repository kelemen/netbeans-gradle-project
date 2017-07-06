package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.event.ListenerRef;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.java.JavaExtension;

public final class JavaSourceDirHandler {
    private static final Logger LOGGER = Logger.getLogger(JavaSourceDirHandler.class.getName());

    private final JavaExtension javaExt;
    private final ChangeListenerManager dirsCreatedListeners;

    public JavaSourceDirHandler(JavaExtension javaExt) {
        this.javaExt = javaExt;
        this.dirsCreatedListeners = new GenericChangeListenerManager();
    }

    public ListenerRef addDirsCreatedListener(Runnable listener) {
        return dirsCreatedListeners.registerListener(listener);
    }

    private static boolean createDir(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            return false;
        }
        else {
            Files.createDirectories(dir);
            return true;
        }
    }

    private static boolean createDir(File dir) {
        try {
            return createDir(dir.toPath());
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to create new directory: " + dir, ex);
            return false;
        }
    }

    private static boolean createDirs(Collection<File> dirs) {
        boolean created = false;

        for (File dir: dirs){
            if (createDir(dir)) {
                created = true;
            }
        }

        return created;
    }

    private static boolean isEmptyAndExist(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return false;
        }

        try (DirectoryStream<Path> dirContent = Files.newDirectoryStream(dir)) {
            return !dirContent.iterator().hasNext();
        }
    }

    private static boolean deleteDirIfEmpty(Path dir) throws IOException {
        if (isEmptyAndExist(dir)) {
            Files.deleteIfExists(dir);
            return true;
        }
        else {
            return false;
        }
    }

    private static boolean deleteDirIfEmpty(File dir) {
        try {
            return deleteDirIfEmpty(dir.toPath());
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to delete: " + dir, ex);
            return false;
        }
    }

    private static boolean deleteDirsIfEmpty(Collection<File> dirs) {
        boolean deleted = false;

        for (File dir: dirs){
            if (deleteDirIfEmpty(dir)) {
                deleted = true;
            }
        }

        return deleted;
    }

    private void fireDirsCreated() {
        dirsCreatedListeners.fireEventually();
    }

    public void deleteEmptyDirectories() {
        NbJavaModule module = javaExt.getCurrentModel().getMainModule();
        boolean changed = false;
        try {
            for (JavaSourceSet sourceSet: module.getSources()) {
                for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                    if (deleteDirsIfEmpty(sourceGroup.getSourceRoots())) {
                        changed = true;
                    }
                }
            }
        } finally {
            if (changed) {
                fireDirsCreated();
            }
        }
    }

    public void createDirectories() {
        NbJavaModule module = javaExt.getCurrentModel().getMainModule();
        boolean changed = false;

        try {
            for (JavaSourceSet sourceSet: module.getSources()) {
                for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                    if (createDirs(sourceGroup.getSourceRoots())) {
                        changed = true;
                    }
                }
            }

            for (NbListedDir listedDir: module.getListedDirs()) {
                if (createDir(listedDir.getDirectory())) {
                    changed = true;
                }
            }
        } finally {
            if (changed) {
                fireDirsCreated();
            }
        }
    }
}
