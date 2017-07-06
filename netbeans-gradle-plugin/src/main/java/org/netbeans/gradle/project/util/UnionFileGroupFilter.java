package org.netbeans.gradle.project.util;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

public final class UnionFileGroupFilter implements FileGroupFilter, Serializable {
    private static final long serialVersionUID = 1L;

    private final FileGroupFilter filter1;
    private final FileGroupFilter filter2;

    private UnionFileGroupFilter(FileGroupFilter filter1, FileGroupFilter filter2) {
        this.filter1 = Objects.requireNonNull(filter1, "filter1");
        this.filter2 = Objects.requireNonNull(filter2, "filter2");
    }

    public static FileGroupFilter union(FileGroupFilter filter1, FileGroupFilter filter2) {
        if (filter1.isAllowAll() || filter2.isAllowAll()) {
            return ExcludeIncludeRules.ALLOW_ALL;
        }

        return new UnionFileGroupFilter(filter1, filter2);
    }

    @Override
    public boolean isIncluded(Path rootPath, Path file) {
        return filter1.isIncluded(rootPath, file) || filter2.isIncluded(rootPath, file);
    }

    @Override
    public boolean isAllowAll() {
        return filter1.isAllowAll() || filter2.isAllowAll();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.filter1);
        hash = 37 * hash + Objects.hashCode(this.filter2);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final UnionFileGroupFilter other = (UnionFileGroupFilter)obj;
        return Objects.equals(this.filter1, other.filter1)
                && Objects.equals(this.filter2, other.filter2);
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final FileGroupFilter filter1;
        private final FileGroupFilter filter2;

        public SerializedFormat(UnionFileGroupFilter source) {
            this.filter1 = source.filter1;
            this.filter2 = source.filter2;
        }

        private Object readResolve() throws ObjectStreamException {
            return new UnionFileGroupFilter(filter1, filter2);
        }
    }
}
