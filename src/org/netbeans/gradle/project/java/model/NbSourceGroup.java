package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.gradle.project.CollectionUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

public final class NbSourceGroup {
    public static final NbSourceGroup EMPTY = new NbSourceGroup(Collections.<NbSourceRoot>emptyList());

    private final List<NbSourceRoot> paths;

    public NbSourceGroup(List<NbSourceRoot> paths) {
        if (paths == null) throw new NullPointerException("paths");
        this.paths = CollectionUtils.copyNullSafeList(paths);
    }

    public List<NbSourceRoot> getPaths() {
        return paths;
    }

    public List<File> getFiles() {
        List<File> result = new ArrayList<File>(paths.size());
        for (NbSourceRoot root: paths) {
            result.add(root.getPath());
        }
        return result;
    }

    public List<FileObject> getFileObjects() {
        List<FileObject> result = new ArrayList<FileObject>(paths.size());
        for (NbSourceRoot root: paths) {
            FileObject fileObject = FileUtil.toFileObject(root.getPath());
            if (fileObject != null) {
                result.add(fileObject);
            }
        }
        return result;
    }

    public List<URI> getUris() {
        List<URI> result = new ArrayList<URI>(paths.size());
        for (NbSourceRoot root: paths) {
            result.add(Utilities.toURI(root.getPath()));
        }
        return result;
    }
}
