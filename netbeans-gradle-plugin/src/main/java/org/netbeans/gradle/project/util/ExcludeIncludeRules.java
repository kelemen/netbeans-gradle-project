package org.netbeans.gradle.project.util;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.openide.filesystems.FileObject;

public final class ExcludeIncludeRules implements Serializable {
    private static final long serialVersionUID = 1L;

    public static ExcludeIncludeRules ALLOW_ALL = new ExcludeIncludeRules(
            Collections.<String>emptySet(),
            Collections.<String>emptySet());

    private final Set<String> excludePatterns;
    private final Set<String> includePatterns;

    private ExcludeIncludeRules(Set<String> excludePatterns, Set<String> includePatterns) {
        this.excludePatterns = CollectionUtils.copyToLinkedHashSet(excludePatterns);
        this.includePatterns = CollectionUtils.copyToLinkedHashSet(includePatterns);

        ExceptionHelper.checkNotNullElements(excludePatterns, "excludePatterns");
        ExceptionHelper.checkNotNullElements(includePatterns, "includePatterns");
    }

    public static ExcludeIncludeRules create(Set<String> excludePatterns, Set<String> includePatterns) {
        if (excludePatterns.isEmpty() && includePatterns.isEmpty()) {
            return ALLOW_ALL;
        }

        return new ExcludeIncludeRules(excludePatterns, includePatterns);
    }

    public static ExcludeIncludeRules create(JavaSourceGroup sourceGroup) {
        return create(sourceGroup.getExcludes(), sourceGroup.getIncludes());
    }

    public Set<String> getExcludePatterns() {
        return excludePatterns;
    }

    public Set<String> getIncludePatterns() {
        return includePatterns;
    }

    public boolean isIncluded(Path rootPath, FileObject file) {
        ExceptionHelper.checkNotNullArgument(rootPath, "rootPath");
        ExceptionHelper.checkNotNullArgument(file, "file");

        Path path = GradleFileUtils.toPath(file);
        return path != null ? isIncluded(rootPath, path) : true;
    }

    public boolean isIncluded(Path rootPath, File file) {
        ExceptionHelper.checkNotNullArgument(rootPath, "rootPath");
        ExceptionHelper.checkNotNullArgument(file, "file");

        return isIncluded(rootPath, file.toPath());
    }

    public boolean isIncluded(Path rootPath, Path file) {
        ExceptionHelper.checkNotNullArgument(rootPath, "rootPath");
        ExceptionHelper.checkNotNullArgument(file, "file");

        return ExcludeInclude.includeFile(
                file,
                rootPath,
                excludePatterns,
                includePatterns);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.excludePatterns);
        hash = 89 * hash + Objects.hashCode(this.includePatterns);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ExcludeIncludeRules other = (ExcludeIncludeRules)obj;
        return Objects.equals(this.excludePatterns, other.excludePatterns)
                && Objects.equals(this.includePatterns, other.includePatterns);
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Set<String> excludePatterns;
        private final Set<String> includePatterns;

        public SerializedFormat(ExcludeIncludeRules source) {
            this.excludePatterns = source.excludePatterns;
            this.includePatterns = source.includePatterns;
        }

        private Object readResolve() throws ObjectStreamException {
            return ExcludeIncludeRules.create(excludePatterns, includePatterns);
        }
    }
}
