package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.List;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.openide.util.Lookup;

public interface NbGradleProjectExtensions {
    public Lookup getCombinedExtensionLookup();
    public List<NbGradleExtensionRef> getExtensionRefs();

    // TODO: Provide default implementations for these in Java 8.
    public <T> Collection<? extends T> lookupAllExtensionObjs(Class<T> type);
    public <T> T lookupExtensionObj(Class<T> type);
    public boolean hasExtension(String extensionName);
}
