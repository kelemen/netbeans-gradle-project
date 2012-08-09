package org.netbeans.gradle.project.model;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openide.filesystems.FileObject;

public final class NbSourceGroup {
    public static final NbSourceGroup EMPTY = new NbSourceGroup(Collections.<URI>emptyList());

    private final List<URI> uris;

    public NbSourceGroup(List<URI> uris) {
        if (uris == null) throw new NullPointerException("uris");
        this.uris = Collections.unmodifiableList(new ArrayList<URI>(uris));
    }

    public List<File> getFiles() {
        List<File> result = new ArrayList<File>(uris.size());
        for (URI uri: uris) {
            File file = NbModelUtils.uriToFile(uri);
            if (file != null) {
                result.add(file);
            }
        }
        return result;
    }

    public List<FileObject> getFileObjects() {
        List<FileObject> result = new ArrayList<FileObject>(uris.size());
        for (URI uri: uris) {
            FileObject fileObject = NbModelUtils.uriToFileObject(uri);
            if (fileObject != null) {
                result.add(fileObject);
            }
        }
        return result;
    }

    public List<URI> getUris() {
        return uris;
    }
}
