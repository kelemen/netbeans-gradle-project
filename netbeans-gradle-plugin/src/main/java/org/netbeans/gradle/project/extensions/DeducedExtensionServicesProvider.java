package org.netbeans.gradle.project.extensions;

import org.openide.util.Lookup;

public interface DeducedExtensionServicesProvider {
    public Lookup getDeducedLookup(NbGradleExtensionRef extensionRef, Lookup... lookups);
}
