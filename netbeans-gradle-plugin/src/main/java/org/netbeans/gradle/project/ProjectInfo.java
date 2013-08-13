package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ProjectInfo {
    private final List<Entry> entries;

    public ProjectInfo(Collection<Entry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
        for (Entry entry: entries) {
            if (entry == null) throw new NullPointerException("entry");
        }
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static final class Entry {
        private final Kind kind;
        private final String info;

        public Entry(Kind kind, String info) {
            if (kind == null) throw new NullPointerException("kind");
            if (info == null) throw new NullPointerException("info");
            this.kind = kind;
            this.info = info;
        }

        public Kind getKind() {
            return kind;
        }

        public String getInfo() {
            return info;
        }
    }

    public enum Kind {
        INFO(1),
        WARNING(2),
        ERROR(3);

        private final int importance;

        private Kind(int importance) {
            this.importance = importance;
        }

        public int getImportance() {
            return importance;
        }
    }
}
