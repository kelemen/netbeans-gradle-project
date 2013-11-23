package org.netbeans.gradle.model.util;

import java.io.Closeable;
import java.io.File;

public interface TemporaryFileRef extends Closeable {
    public File getFile();
}
