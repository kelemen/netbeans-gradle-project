package org.netbeans.gradle.model.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

// This class assumes that files in NB_GRADLE_TEMP_DIR are only used by this class.
// An external agent is allowed to delete the files if it can.
public final class StringAsFileRef implements Closeable {
    private final TemporaryFileRef fileRef;

    private StringAsFileRef(TemporaryFileRef fileRef) {
        this.fileRef = fileRef;
    }

    public static StringAsFileRef createRef(String name, String content, Charset encoding) throws IOException {
        return new StringAsFileRef(TemporaryFileManager.getDefault().createFile(name, content, encoding.name()));
    }

    public File getFile() {
        return fileRef.getFile();
    }

    @Override
    public String toString() {
        return "StrFileReference{" + fileRef.getFile() + "}";
    }

    public void close() throws IOException {
        fileRef.close();
    }
}
