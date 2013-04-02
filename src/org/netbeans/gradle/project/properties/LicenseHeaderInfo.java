package org.netbeans.gradle.project.properties;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.gradle.project.NbGradleProject;

public final class LicenseHeaderInfo {
    private final String licenseName;
    private final Map<String, String> properties;
    private final File licenseTemplateFile;

    public LicenseHeaderInfo(
            String licenseName,
            Map<String, String> properties,
            File licenseTemplateFile) {
        if (licenseName == null) throw new NullPointerException("licenseName");
        if (properties == null) throw new NullPointerException("properties");

        this.licenseName = licenseName;
        this.properties = Collections.unmodifiableMap(new HashMap<String, String>(properties));
        this.licenseTemplateFile = licenseTemplateFile;
    }

    public File getLicenseTemplateFile() {
        return licenseTemplateFile;
    }

    public File getLicenseTemplateFile(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        if (licenseTemplateFile.isAbsolute()) {
            return licenseTemplateFile;
        }

        File rootProjectDir = project.getAvailableModel().getRootProjectDir();
        return new File(rootProjectDir, licenseTemplateFile.getPath());
    }

    public String getLicenseName() {
        return licenseName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
