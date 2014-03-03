package org.netbeans.gradle.project.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
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

    public static void deleteDirectory(final CancellationToken cancelToken, Path directory) throws IOException {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(directory, "directory");

        Files.walkFileTree(directory, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                cancelToken.checkCanceled();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                cancelToken.checkCanceled();
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                cancelToken.checkCanceled();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                cancelToken.checkCanceled();
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteDirectory(CancellationToken cancelToken, FileObject directory) throws IOException {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(directory, "directory");

        File asFile = FileUtil.toFile(directory);
        if (asFile == null) {
            throw new FileNotFoundException("Cannot find " + directory);
        }

        deleteDirectory(cancelToken, asFile.toPath());
    }

    private NbFileUtils() {
        throw new AssertionError();
    }
}
