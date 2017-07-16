package org.netbeans.gradle.project.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.others.ChangeLFPlugin;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class NbFileUtils {
    private static final Logger LOGGER = Logger.getLogger(NbFileUtils.class.getName());

    private static final int FILE_BUFFER_SIZE = 8 * 1024;

    private static boolean isLineEndingByte(byte ch) {
        return ch == 13 || ch == 10;
    }

    public static String tryGetLineSeparatorForTextFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }

        // This method won't work with every encoding but we save properties
        // file with UTF-8, with which this should be fine.
        byte prevChar = 0;
        byte[] buffer = new byte[FILE_BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(file)) {
            int readCount = input.read(buffer);
            while (readCount > 0) {
                for (int i = 0; i < readCount; i++) {
                    byte ch = buffer[i];
                    if (isLineEndingByte(prevChar)) {
                        switch (ch) {
                            case 10:
                                return prevChar == 13 ? "\r\n" : "\n";
                            case 13:
                                // \n\r is not valid, returning null is safer.
                                return prevChar == 10 ? null : "\r";
                            default:
                                return Character.toString((char)prevChar);
                        }
                    }

                    prevChar = ch;
                }

                readCount = input.read(buffer);
            }

            return isLineEndingByte(prevChar) ? Character.toString((char)prevChar) : null;
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to read config file for determining its line separator", ex);
            return null;
        }
    }

    public static String tryMakeRelative(File parent, File file) {
        File normParent = FileUtil.normalizeFile(parent);
        File normFile = FileUtil.normalizeFile(file);

        if (normParent == null || normFile == null) {
            return null;
        }

        FileObject parentObj = FileUtil.toFileObject(normParent);
        FileObject fileObj = FileUtil.toFileObject(normFile);

        if (fileObj == null || parentObj == null) {
            return null;
        }

        String relPath = FileUtil.getRelativePath(parentObj, fileObj);
        return relPath != null ? relPath.replace("/", File.separator) : null;
    }

    private static boolean isSafeChar(char ch) {
        if (ch >= 'A' && ch <= 'Z') return true;
        if (ch >= 'a' && ch <= 'z') return true;
        if (ch >= '0' && ch <= '9') return true;

        return "_-$. ".indexOf(ch) >= 0;
    }

    public static String toSafeFileName(String name) {
        ExceptionHelper.checkNotNullArgument(name, "name");

        StringBuilder result = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            result.append(isSafeChar(ch) ? ch : "_");
        }
        return result.toString();
    }

    public static ListenerRef addDirectoryContentListener(
            final FileObject dir,
            final Runnable listener) {
        return addDirectoryContentListener(dir, false, listener);
    }

    public static ListenerRef addDirectoryContentListener(
            final FileObject dir,
            final boolean listenForDirs,
            final Runnable listener) {
        ExceptionHelper.checkNotNullArgument(dir, "dir");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final FileChangeListener fileChangeListener = new FileChangeAdapter() {
            @Override
            public void fileFolderCreated(FileEvent fe) {
                if (listenForDirs) {
                    listener.run();
                }

            }
            @Override
            public void fileDeleted(FileEvent fe) {
                listener.run();
            }

            @Override
            public void fileDataCreated(FileEvent fe) {
                listener.run();
            }
        };
        dir.addFileChangeListener(fileChangeListener);
        return NbListenerRefs.fromRunnable(new Runnable() {
            @Override
            public void run() {
                dir.removeFileChangeListener(fileChangeListener);
            }
        });
    }

    public static URL getSafeURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String getFileNameStr(Path path) {
        Path result = path.getFileName();
        return result != null ? result.toString() : "";
    }

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
        if (subDirs == null) {
            return null;
        }

        for (File dir: subDirs) {
            File srcFileName = new File(dir, fileName);
            if (srcFileName.isFile()) {
                return asArchiveOrDir(FileUtil.toFileObject(srcFileName));
            }
        }
        return null;
    }

    public static Path asPath(FileObject fileObj) {
        return fileObj != null ? asPath(FileUtil.toFile(fileObj)) : null;
    }

    public static Path asPath(File file) {
        try {
            return file != null ? file.toPath() : null;
        } catch (InvalidPathException ex) {
            return null;
        }
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

    public static void writeLinesToFile(
            Path file,
            Collection<String> lines,
            Charset charset) throws IOException {
        writeLinesToFile(file, lines, charset, (Project)null);
    }

    public static void writeLinesToFile(
            Path file,
            Collection<String> lines,
            Charset charset,
            Project ownerProject) throws IOException {
        String lineSeparator = ChangeLFPlugin.getPreferredLineSeparator(ownerProject);
        writeLinesToFile(file, lines, charset, lineSeparator);
    }

    public static void writeLinesToFile(
            Path file,
            Collection<String> lines,
            Charset charset,
            String lineSeparator) throws IOException {
        ExceptionHelper.checkNotNullArgument(file, "file");
        ExceptionHelper.checkNotNullElements(lines, "lines");
        ExceptionHelper.checkNotNullArgument(charset, "charset");

        if (lineSeparator == null) {
            Files.write(file, lines, charset);
        }
        else {
            byte[] lineSeparatorBytes = lineSeparator.getBytes(charset);
            try (OutputStream fileOutput = Files.newOutputStream(file);
                    OutputStream output = new BufferedOutputStream(fileOutput, 16 * 1024)) {
                for (String line: lines) {
                    output.write(line.getBytes(charset));
                    output.write(lineSeparatorBytes);
                }
            }
        }
    }

    public static Path toSafeRealPath(Path src) {
        try {
            return src.toRealPath();
        } catch (IOException ex) {
            return src.normalize();
        }
    }

    private NbFileUtils() {
        throw new AssertionError();
    }
}
