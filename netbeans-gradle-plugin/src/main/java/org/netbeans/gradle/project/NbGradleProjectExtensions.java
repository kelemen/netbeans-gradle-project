package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

public final class NbGradleProjectExtensions {
    public static final NbGradleProjectExtensions EMPTY
            = new NbGradleProjectExtensions(Collections.<NbGradleExtensionRef>emptyList());

    private final List<NbGradleExtensionRef> extensionRefs;
    private final Set<String> extensionNames;
    private final Lookup combinedExtensionLookup;

    public NbGradleProjectExtensions(Collection<? extends NbGradleExtensionRef> extensions) {
        this.extensionRefs = Collections.unmodifiableList(new ArrayList<>(extensions));

        Set<String> newExtensionNames = CollectionUtils.newHashSet(extensions.size());
        for (NbGradleExtensionRef extension: this.extensionRefs) {
            newExtensionNames.add(extension.getName());
        }
        this.extensionNames = Collections.unmodifiableSet(newExtensionNames);

        this.combinedExtensionLookup = combineExtensionLookups(this.extensionRefs);
    }

    public <T> Collection<? extends T> lookupAllExtensionObjs(Class<T> type) {
        return combinedExtensionLookup.lookupAll(type);
    }

    public <T> T lookupExtensionObj(Class<T> type) {
        return combinedExtensionLookup.lookup(type);
    }

    public Lookup getCombinedExtensionLookup() {
        return combinedExtensionLookup;
    }

    public List<NbGradleExtensionRef> getExtensionRefs() {
        return extensionRefs;
    }

    public boolean hasExtension(String extensionName) {
        return extensionNames.contains(extensionName);
    }

    private static Lookup combineExtensionLookups(Collection<? extends NbGradleExtensionRef> extensions) {
        List<Lookup> extensionLookups = new ArrayList<>(extensions.size());
        for (NbGradleExtensionRef extenion: extensions) {
            extensionLookups.add(extenion.getExtensionLookup());
        }
        return new ProxyLookup(extensionLookups.toArray(new Lookup[extensionLookups.size()]));
    }
}
