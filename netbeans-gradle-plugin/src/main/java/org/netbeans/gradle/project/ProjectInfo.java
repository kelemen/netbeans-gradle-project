package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public final class ProjectInfo {
    private final List<Entry> entries;

    public ProjectInfo(Collection<Entry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));

        ExceptionHelper.checkNotNullElements(this.entries, "entries");
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static final class Entry {
        private final Kind kind;
        private final String info;

        public Entry(Kind kind, String info) {
            ExceptionHelper.checkNotNullArgument(kind, "kind");
            ExceptionHelper.checkNotNullArgument(info, "info");

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
