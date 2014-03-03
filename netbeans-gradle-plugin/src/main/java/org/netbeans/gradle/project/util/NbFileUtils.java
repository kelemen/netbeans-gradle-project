package org.netbeans.gradle.project.util;

import java.io.File;
import java.io.FileFilter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class NbFileUtils {
    public static FileObject asArchiveOrDir(File file) {
        FileObject result = FileUtil.toFileObject(file);
        return asArchiveOrDir(result);
    }

    public static FileObject asArchiveOrDir(FileObject file) {
        if (file == null) {
            return null;
        }

        if (FileUtil.isArchiveFile(file)) {
            return FileUtil.getArchiveRoot(file);
        }
        else {
            return file;
        }
    }

    public static FileObject getFileFromASubDir(FileObject root, String fileName) {
        File rootDir = FileUtil.toFile(root);
        if (rootDir == null) {
            return null;
        }

        File[] subDirs = rootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File dir: subDirs) {
            File srcFileName = new File(dir, fileName);
            if (srcFileName.isFile()) {
                return asArchiveOrDir(FileUtil.toFileObject(srcFileName));
            }
        }
        return null;
    }

    public static File asFile(FileObject fileObj) {
        return fileObj != null ? FileUtil.toFile(fileObj) : null;
    }

    public static FileObject asFileObject(File file) {
        return file != null ? FileUtil.toFileObject(file) : null;
    }

    public static boolean isParentOrSame(File parent, File child) {
        for (File current = child; current != null; current = current.getParentFile()) {
            if (current.equals(parent)) {
                return true;
            }
        }
        return false;
    }

    private NbFileUtils() {
        throw new AssertionError();
    }
}
