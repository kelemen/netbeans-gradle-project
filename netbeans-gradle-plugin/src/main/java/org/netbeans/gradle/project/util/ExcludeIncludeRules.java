package org.netbeans.gradle.project.util;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.SourceIncludePatterns;
import org.openide.filesystems.FileObject;

public final class ExcludeIncludeRules implements FileGroupFilter, Serializable {
    private static final long serialVersionUID = 1L;

    public static ExcludeIncludeRules ALLOW_ALL = new ExcludeIncludeRules(
            SourceIncludePatterns.ALLOW_ALL);

    private final SourceIncludePatterns sourceIncludePatterns;

    private ExcludeIncludeRules(SourceIncludePatterns sourceIncludePatterns) {
        this.sourceIncludePatterns = Objects.requireNonNull(sourceIncludePatterns, "sourceIncludePatterns");
    }

    public static ExcludeIncludeRules create(SourceIncludePatterns sourceIncludePatterns) {
        if (sourceIncludePatterns.isAllowAll()) {
            return ALLOW_ALL;
        }

        return new ExcludeIncludeRules(sourceIncludePatterns);
    }

    public static ExcludeIncludeRules create(JavaSourceGroup sourceGroup) {
        return create(sourceGroup.getExcludePatterns());
    }

    @Override
    public boolean isAllowAll() {
        return sourceIncludePatterns.isAllowAll();
    }

    public SourceIncludePatterns getSourceIncludePatterns() {
        return sourceIncludePatterns;
    }

    public boolean isIncluded(Path rootPath, FileObject file) {
        Objects.requireNonNull(rootPath, "rootPath");
        Objects.requireNonNull(file, "file");

        if (isAllowAll()) {
            return true;
        }

        Path path = GradleFileUtils.toPath(file);
        return path != null ? isIncluded(rootPath, path) : true;
    }

    public boolean isIncluded(Path rootPath, File file) {
        Objects.requireNonNull(rootPath, "rootPath");
        Objects.requireNonNull(file, "file");

        if (isAllowAll()) {
            return true;
        }

        return isIncluded(rootPath, file.toPath());
    }

    @Override
    public boolean isIncluded(Path rootPath, Path file) {
        Objects.requireNonNull(rootPath, "rootPath");
        Objects.requireNonNull(file, "file");

        if (isAllowAll()) {
            return true;
        }

        return ExcludeInclude.includeFile(
                file,
                rootPath,
                sourceIncludePatterns.getExcludePatterns(),
                sourceIncludePatterns.getIncludePatterns());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.sourceIncludePatterns);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ExcludeIncludeRules other = (ExcludeIncludeRules)obj;
        return Objects.equals(this.sourceIncludePatterns, other.sourceIncludePatterns);
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final SourceIncludePatterns sourceIncludePatterns;

        public SerializedFormat(ExcludeIncludeRules source) {
            this.sourceIncludePatterns = source.sourceIncludePatterns;
        }

        private Object readResolve() throws ObjectStreamException {
            return ExcludeIncludeRules.create(sourceIncludePatterns);
        }
    }
}
