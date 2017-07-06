package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import org.netbeans.gradle.project.util.FileGroupFilter;
import org.netbeans.gradle.project.util.UrlFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.FilteringPathResourceImplementation;
import org.netbeans.spi.java.classpath.support.PathResourceBase;

final class ExcludeAwarePathResource extends PathResourceBase implements FilteringPathResourceImplementation {
    private final Path root;
    private final URL url;
    private final FileGroupFilter includeRules;

    private ExcludeAwarePathResource(File root, URL rootUrl, FileGroupFilter includeRules) {
        this.root = root.toPath();
        this.url = rootUrl;
        this.includeRules = includeRules;
    }

    public static ExcludeAwarePathResource tryCreate(
            File root,
            FileGroupFilter includeRules,
            UrlFactory urlForArchiveFactory) {
        Objects.requireNonNull(includeRules, "includeRules");

        URL url = urlForArchiveFactory.toUrl(root);
        if (url == null) {
            return null;
        }
        return new ExcludeAwarePathResource(root, url, includeRules);
    }

    @Override
    public URL[] getRoots() {
        return new URL[]{url};
    }

    @Override
    public ClassPathImplementation getContent() {
        return null;
    }

    @Override
    public boolean includes(URL urlRoot, String resource) {
        String normPath = resource.replace("/", root.getFileSystem().getSeparator());
        Path resourcePath = root.resolve(normPath);
        return includeRules.isIncluded(root, resourcePath);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.root);
        hash = 43 * hash + Objects.hashCode(this.includeRules);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (getClass() != obj.getClass())
            return false;
        final ExcludeAwarePathResource other = (ExcludeAwarePathResource)obj;
        return Objects.equals(this.root, other.root) && Objects.equals(this.includeRules, other.includeRules);
    }

    @Override
    public String toString() {
        return "ExcludeAwarePathResource{" + url + "}";
    }
}
