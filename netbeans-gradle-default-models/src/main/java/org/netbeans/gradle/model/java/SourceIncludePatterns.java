package org.netbeans.gradle.model.java;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class SourceIncludePatterns implements Serializable {
    private static final long serialVersionUID = 1L;

    public static SourceIncludePatterns ALLOW_ALL = new SourceIncludePatterns(
            Collections.<String>emptySet(),
            Collections.<String>emptySet());

    private final Set<String> excludePatterns;
    private final Set<String> includePatterns;

    private SourceIncludePatterns(
            Collection<? extends String> excludePatterns,
            Collection<? extends String> includePatterns) {

        this.excludePatterns = CollectionUtils.copyToLinkedHashSet(excludePatterns);
        this.includePatterns = CollectionUtils.copyToLinkedHashSet(includePatterns);

        CollectionUtils.checkNoNullElements(excludePatterns, "excludePatterns");
        CollectionUtils.checkNoNullElements(includePatterns, "includePatterns");
    }

    public static SourceIncludePatterns create(
            Collection<? extends String> excludePatterns,
            Collection<? extends String> includePatterns) {
        if (excludePatterns.isEmpty() && includePatterns.isEmpty()) {
            return ALLOW_ALL;
        }

        return new SourceIncludePatterns(excludePatterns, includePatterns);
    }

    public boolean isAllowAll() {
        return excludePatterns.isEmpty() && includePatterns.isEmpty();
    }

    public Set<String> getExcludePatterns() {
        return excludePatterns;
    }

    public Set<String> getIncludePatterns() {
        return includePatterns;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + excludePatterns.hashCode();
        hash = 89 * hash + includePatterns.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final SourceIncludePatterns other = (SourceIncludePatterns)obj;
        return excludePatterns.equals(other.excludePatterns)
                && includePatterns.equals(other.includePatterns);
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

        public SerializedFormat(SourceIncludePatterns source) {
            this.excludePatterns = source.excludePatterns;
            this.includePatterns = source.includePatterns;
        }

        private Object readResolve() throws ObjectStreamException {
            return SourceIncludePatterns.create(excludePatterns, includePatterns);
        }
    }
}
