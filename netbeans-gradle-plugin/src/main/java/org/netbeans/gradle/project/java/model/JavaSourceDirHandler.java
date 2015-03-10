package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;

public final class JavaSourceDirHandler {
    private static final Logger LOGGER = Logger.getLogger(JavaSourceDirHandler.class.getName());

    private final JavaExtension javaExt;
    private final ListenerManager<Runnable> dirsCreatedListeners;

    public JavaSourceDirHandler(JavaExtension javaExt) {
        this.javaExt = javaExt;
        this.dirsCreatedListeners = new CopyOnTriggerListenerManager<>();
    }

    public ListenerRef addDirsCreatedListener(Runnable listener) {
        return dirsCreatedListeners.registerListener(listener);
    }

    private static boolean createDir(File dir) {
        if (dir.isDirectory()) {
            return false;
        }
        boolean created = dir.mkdirs();
        if (!created) {
            LOGGER.log(Level.INFO, "Failed to create new directory: {0}", dir);
        }

        return created;
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

    private static boolean isEmptyAndExist(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }

        // FIXME: Use a more efficient solution in Java 7.
        File[] files = dir.listFiles();
        return files != null && files.length == 0;
    }

    private static boolean deleteDirIfEmpty(File dir) {
        if (!isEmptyAndExist(dir)) {
            return false;
        }

        boolean deleted = dir.delete();
        if (!deleted) {
            LOGGER.log(Level.INFO, "Failed to delete an empty directory: {0}", dir);
        }

        return deleted;
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
        EventListeners.dispatchRunnable(dirsCreatedListeners);
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
