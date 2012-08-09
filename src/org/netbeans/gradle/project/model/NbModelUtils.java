package org.netbeans.gradle.project.model;

import java.io.File;
import java.net.URI;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

public final class NbModelUtils {
    public static File uriToFile(URI uri) {
        if ("file".equals(uri.getScheme())) {
            return Utilities.toFile(uri);
        }
        else {
            return null;
        }
    }

    public static FileObject uriToFileObject(URI uri) {
        File file = uriToFile(uri);
        return file != null ? FileUtil.toFileObject(file) : null;
    }

    private NbModelUtils() {
        throw new AssertionError();
    }
}
