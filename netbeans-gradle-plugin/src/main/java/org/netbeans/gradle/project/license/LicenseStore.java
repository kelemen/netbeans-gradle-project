package org.netbeans.gradle.project.license;

import java.io.IOException;

public interface LicenseStore<L> {
    public void addLicense(L licenseDef) throws IOException;
    public void removeLicense(L licenseDef) throws IOException;

    public boolean containsLicense(String licenseId);
}
