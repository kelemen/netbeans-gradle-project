package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class SourceBinaryMap {
    public static final SourceBinaryMap EMPTY = new Builder().create();

    public static final class Builder {
        private final List<Entry> entries;

        public Builder() {
            this.entries = new LinkedList<Entry>();
        }

        public void addSourceEntry(File binary, SourceEntry sourceEntry) {
            if (sourceEntry.javadoc == null && sourceEntry.source == null) {
                return;
            }

            entries.add(new Entry(binary, sourceEntry));
        }

        public void addSourceEntry(File binary, File source, File javadoc) {
            addSourceEntry(binary, new SourceEntry(source, javadoc));
        }

        public SourceBinaryMap create() {
            return new SourceBinaryMap(this);
        }
    }

    private final Map<File, File> sourceToBinary;
    private final Map<File, SourceEntry> binaryToSource;

    public SourceBinaryMap(Builder builder) {
        int entriesCount = builder.entries.size();

        Map<File, File> sourceToBinaryBuilder = CollectionUtils.newHashMap(entriesCount);
        Map<File, SourceEntry> binaryToSourceBuilder = CollectionUtils.newHashMap(entriesCount);

        for (Entry entry: builder.entries) {
            File sourceFile = entry.sourceEntry.source;
            if (sourceFile != null) {
                sourceToBinaryBuilder.put(sourceFile, entry.binary);
            }

            binaryToSourceBuilder.put(entry.binary, entry.sourceEntry);
        }

        this.binaryToSource = Collections.unmodifiableMap(binaryToSourceBuilder);
        this.sourceToBinary = Collections.unmodifiableMap(sourceToBinaryBuilder);
    }

    public SourceEntry tryGetSourceEntry(File binaryFile) {
        return binaryToSource.get(binaryFile);
    }

    public File tryGetSourceFile(File binaryFile) {
        SourceEntry result = tryGetSourceEntry(binaryFile);
        return result != null ? result.source : null;
    }

    public File tryGetJavadocFile(File binaryFile) {
        SourceEntry result = tryGetSourceEntry(binaryFile);
        return result != null ? result.javadoc : null;
    }

    public Set<File> getAllSourcesFiles() {
        return sourceToBinary.keySet();
    }

    public Set<File> getAllBinaryFiles() {
        return binaryToSource.keySet();
    }

    public static final class SourceEntry {
        private final File source;
        private final File javadoc;

        public SourceEntry(File source, File javadoc) {
            this.source = source;
            this.javadoc = javadoc;
        }

        public File getSource() {
            return source;
        }

        public File getJavadoc() {
            return javadoc;
        }
    }

    private static final class Entry {
        public final File binary;
        public final SourceEntry sourceEntry;

        public Entry(File binary, SourceEntry sourceEntry) {
            if (binary == null) throw new NullPointerException("binary");
            if (sourceEntry == null) throw new NullPointerException("sourceEntry");

            this.binary = binary;
            this.sourceEntry = sourceEntry;
        }
    }
}
