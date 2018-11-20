package org.netbeans.gradle.model.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TemporaryFileManager {
    private static final Logger LOGGER = Logger.getLogger(TemporaryFileManager.class.getName());

    private static final TemporaryFileManager DEFAULT = new TemporaryFileManager();

    private final Lock mainLock;
    private final Map<BinaryContent, FileReference> files;

    public TemporaryFileManager() {
        this.mainLock = new ReentrantLock();
        this.files = new HashMap<BinaryContent, FileReference>();
    }

    public static TemporaryFileManager getDefault() {
        return DEFAULT;
    }

    private TemporaryFileRef tryGetExisting(BinaryContent content) throws IOException {
        mainLock.lock();
        try {
            FileReference fileRef = files.get(content);
            if (fileRef != null) {
                fileRef.useOne();
                return new SingleFileReference(fileRef.key, fileRef);
            }
        } finally {
            mainLock.unlock();
        }

        return null;
    }

    private TemporaryFileRef tryGetExisting(byte[] content) throws IOException {
        return tryGetExisting(new BinaryContent(content, false));
    }

    private static void closeAndDelete(LockedFile file) throws IOException {
        try {
            file.close();
        } finally {
            file.file.delete();
        }
    }

    private TemporaryFileRef createFileGuessUncached(
            String preferredPrefix,
            BinaryContent content) throws IOException {

        TemporaryFileRef result;
        LockedFile file = new LockedFile(preferredPrefix, content);

        mainLock.lock();
        try {
            FileReference fileRef = files.get(content);
            if (fileRef != null) {
                fileRef.useOne();
                result = new SingleFileReference(fileRef.key, fileRef);
            }
            else {
                fileRef = new FileReference(content, file, 1);
                result = new SingleFileReference(content, fileRef);

                file = null;
                files.put(content, fileRef);
            }
        } finally {
            mainLock.unlock();

            if (file != null) {
                closeAndDelete(file);
            }
        }

        return result;
    }

    public TemporaryFileRef createFile(String preferredPrefix, String strContent, Charset charset) throws IOException {
        BinaryContent content = new BinaryContent(strContent.getBytes(charset.name()), false);
        return createFile(preferredPrefix, content);
    }

    public TemporaryFileRef createFile(String preferredPrefix, String strContent, String charsetName) throws IOException {
        BinaryContent content = new BinaryContent(strContent.getBytes(charsetName), false);
        return createFile(preferredPrefix, content);
    }

    public TemporaryFileRef createFileFromSerialized(String preferredPrefix, Object contentObj) throws IOException {
        BinaryContent content = new BinaryContent(SerializationUtils.serializeObject(contentObj), false);
        return createFile(preferredPrefix, content);
    }

    private TemporaryFileRef createFile(String preferredPrefix, BinaryContent content) throws IOException {
        TemporaryFileRef result = tryGetExisting(content);
        if (result != null) {
            return result;
        }

        return createFileGuessUncached(preferredPrefix, content);
    }

    public TemporaryFileRef createFile(String preferredPrefix, byte[] content) throws IOException {
        TemporaryFileRef result = tryGetExisting(content);
        if (result != null) {
            return result;
        }

        return createFileGuessUncached(preferredPrefix, new BinaryContent(content));
    }

    private static final class LockedFile implements Closeable {
        public final File file;
        private final RandomAccessFile lockedRef;

        public LockedFile(String namePrefix, BinaryContent content) throws IOException {
            file = BasicFileUtils.createTmpFile(
                    namePrefix + "-" + BasicFileUtils.getMD5(content.content), ".tmp");
            try {
                lockedRef = new RandomAccessFile(file, "rw");
                lockedRef.write(content.content);
                lockedRef.getFD().sync();
            } catch (Throwable ex) {
                if (!file.delete()) {
                    LOGGER.log(Level.WARNING, "Failed to remove temporary file: {0}", file);
                }
                throw Exceptions.throwUncheckedIO(ex);
            }
        }

        @Override
        public void close() throws IOException {
            lockedRef.close();
        }
    }

    private final class SingleFileReference implements TemporaryFileRef {
        private final BinaryContent content;
        private final FileReference fileRef;
        private final ObjectFinalizer finalizer;

        public SingleFileReference(BinaryContent content, FileReference fileRef) {
            this.content = content;
            this.fileRef = fileRef;
            this.finalizer = new ObjectFinalizer(new Runnable() {
                @Override
                public void run() {
                    doClose();
                }
            }, "SingleFileReference{" + fileRef.getFile() + "}");
        }

        @Override
        public File getFile() {
            return fileRef.getFile();
        }

        private void doClose() {
            boolean delete = false;

            mainLock.lock();
            try {
                if (fileRef.releaseOne()) {
                    files.remove(content);
                    delete = true;
                }
            } finally {
                mainLock.unlock();
            }

            if (delete) {
                try {
                    closeAndDelete(fileRef.getLockedFile());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public void close() throws IOException {
            finalizer.doFinalize();
        }

        @Override
        public String toString() {
            return "TmpFileRef{" + fileRef.getFile() + "}";
        }
    }

    private static final class FileReference {
        public final BinaryContent key;
        private final LockedFile file;
        private int useCount;

        public FileReference(BinaryContent key, LockedFile file, int useCount) {
            this.key = key;
            this.file = file;
            this.useCount = useCount;
        }

        public File getFile() {
            return file.file;
        }

        public LockedFile getLockedFile() {
            return file;
        }

        public void useOne() {
            useCount++;
        }

        public boolean releaseOne() {
            useCount--;
            return useCount == 0;
        }
    }

    private static final class BinaryContent {
        private final byte[] content;
        private final int hash;

        public BinaryContent(byte[] content) {
            this(content, true);
        }

        public BinaryContent(byte[] content, boolean clone) {
            this.content = clone ? content.clone() : content;
            this.hash = 679 + Arrays.hashCode(this.content);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final BinaryContent other = (BinaryContent)obj;
            return Arrays.equals(this.content, other.content);
        }
    }
}
