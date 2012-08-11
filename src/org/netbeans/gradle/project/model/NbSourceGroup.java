package org.netbeans.gradle.project.model;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

public final class NbSourceGroup {
    public static final NbSourceGroup EMPTY = new NbSourceGroup(Collections.<File>emptyList());

    private final List<File> paths;

    public NbSourceGroup(List<File> paths) {
        if (paths == null) throw new NullPointerException("paths");
        this.paths = Collections.unmodifiableList(new ArrayList<File>(paths));
    }

    public List<File> getPaths() {
        return paths;
    }

    public List<FileObject> getFileObjects() {
        List<FileObject> result = new ArrayList<FileObject>(paths.size());
        for (File path: paths) {
            FileObject fileObject = FileUtil.toFileObject(path);
            if (fileObject != null) {
                result.add(fileObject);
            }
        }
        return result;
    }

    public List<URI> getUris() {
        List<URI> result = new ArrayList<URI>(paths.size());
        for (File path: paths) {
            result.add(Utilities.toURI(path));
        }
        return result;
    }
}
