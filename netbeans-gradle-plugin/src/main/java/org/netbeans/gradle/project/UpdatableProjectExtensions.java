package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.openide.util.Lookup;

final class UpdatableProjectExtensions implements NbGradleProjectExtensions {
    private final Lookup combinedLookup;
    private volatile ExtensionCollection extensions;

    public UpdatableProjectExtensions(Lookup combinedLookup) {
        this.combinedLookup = combinedLookup;
        this.extensions = ExtensionCollection.EMPTY;
    }

    public void setExtensions(Collection<? extends NbGradleExtensionRef> extensions) {
        this.extensions = new ExtensionCollection(extensions);
    }

    @Override
    public <T> Collection<? extends T> lookupAllExtensionObjs(Class<T> type) {
        return combinedLookup.lookupAll(type);
    }

    @Override
    public <T> T lookupExtensionObj(Class<T> type) {
        return combinedLookup.lookup(type);
    }

    @Override
    public Lookup getCombinedExtensionLookup() {
        return combinedLookup;
    }

    @Override
    public List<NbGradleExtensionRef> getExtensionRefs() {
        return extensions.getExtensionRefs();
    }

    @Override
    public boolean hasExtension(String extensionName) {
        return extensions.hasExtension(extensionName);
    }

    private static final class ExtensionCollection {
        public static final ExtensionCollection EMPTY
                = new ExtensionCollection(Collections.<NbGradleExtensionRef>emptySet());

        private final List<NbGradleExtensionRef> extensionRefs;
        private final Set<String> extensionNames;

        public ExtensionCollection(Collection<? extends NbGradleExtensionRef> extensions) {
            this.extensionRefs = Collections.unmodifiableList(new ArrayList<>(extensions));

            Set<String> newExtensionNames = CollectionUtils.newHashSet(extensions.size());
            for (NbGradleExtensionRef extension: this.extensionRefs) {
                newExtensionNames.add(extension.getName());
            }
            this.extensionNames = Collections.unmodifiableSet(newExtensionNames);
        }

        public List<NbGradleExtensionRef> getExtensionRefs() {
            return extensionRefs;
        }

        public boolean hasExtension(String extensionName) {
            return extensionNames.contains(extensionName);
        }
    }
}
