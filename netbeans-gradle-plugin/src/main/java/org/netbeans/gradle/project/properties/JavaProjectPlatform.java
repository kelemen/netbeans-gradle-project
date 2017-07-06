package org.netbeans.gradle.project.properties;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.openide.filesystems.FileObject;
import org.openide.modules.SpecificationVersion;

public final class JavaProjectPlatform implements ProjectPlatform {
    private final JavaPlatform platform;

    public JavaProjectPlatform(JavaPlatform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    private static Collection<URL> urlsOfClassPath(ClassPath classPath) {
        List<ClassPath.Entry> entries = classPath.entries();
        List<URL> urls = new ArrayList<>(entries.size());
        for (ClassPath.Entry entry: entries) {
            urls.add(entry.getURL());
        }
        return urls;
    }

    @Override
    public FileObject getRootFolder() {
        for (FileObject folder: platform.getInstallFolders()) {
            return folder;
        }
        return null;
    }

    @Override
    public Collection<URL> getBootLibraries() {
        return urlsOfClassPath(platform.getBootstrapLibraries());
    }

    @Override
    public Collection<URL> getSourcePaths() {
        return urlsOfClassPath(platform.getSourceFolders());
    }

    @Override
    public String getDisplayName() {
        return platform.getDisplayName();
    }

    @Override
    public String getName() {
        return platform.getSpecification().getName();
    }

    @Override
    public String getVersion() {
        SpecificationVersion version = platform.getSpecification().getVersion();
        return version != null ? version.toString() : "";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + System.identityHashCode(platform);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final JavaProjectPlatform other = (JavaProjectPlatform)obj;
        return this.platform == other.platform;
    }

}
