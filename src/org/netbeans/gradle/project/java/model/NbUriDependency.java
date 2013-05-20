package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.net.URI;
import org.openide.filesystems.FileObject;

public final class NbUriDependency implements NbJavaDependency {
    private final URI uri;
    private final URI srcUri;
    private final boolean transitive;

    public NbUriDependency(URI uri, URI srcUri, boolean transitive) {
        if (uri == null) throw new NullPointerException("uri");
        this.uri = uri;
        this.srcUri = srcUri;
        this.transitive = transitive;
    }

    public File tryGetAsFile() {
        return NbJavaModelUtils.uriToFile(uri);
    }

    public FileObject tryGetAsFileObject() {
        return NbJavaModelUtils.uriToFileObject(uri);
    }

    public URI getUri() {
        return uri;
    }

    public URI getSrcUri() {
        return srcUri;
    }

    @Override
    public String getShortName() {
        File file = tryGetAsFile();
        String result = file != null ? file.getName() : uri.getPath();
        return result != null ? result : "?";
    }

    @Override
    public boolean isTransitive() {
        return transitive;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + uri.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NbUriDependency other = (NbUriDependency)obj;
        return this.uri.equals(other.uri);
    }
}
