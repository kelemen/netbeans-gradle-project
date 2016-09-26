package org.netbeans.gradle.project.license;

import java.io.IOException;
import java.util.Collection;

public interface LicenseSource {
    public Collection<LicenseRef> getAllLicense() throws IOException;
}
