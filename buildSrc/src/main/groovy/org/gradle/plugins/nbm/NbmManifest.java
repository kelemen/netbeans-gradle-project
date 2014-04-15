package org.gradle.plugins.nbm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NbmManifest {
    private final Map<String, Object> entries;
    private final Map<String, Object> entriesView;

    public NbmManifest() {
        this.entries = new LinkedHashMap<>();
        this.entriesView = Collections.unmodifiableMap(entries);
    }

    public void put(String key, Object value) {
        entries.put(key, value);
    }

    public Map<String, Object> getAllEntries() {
        return entriesView;
    }
}
