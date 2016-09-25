package org.netbeans.gradle.project.license;

import java.nio.file.Path;
import java.util.Map;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class LicenseHeaderInfo {
    private final String licenseName;
    private final Map<String, String> properties;
    private final Path licenseTemplateFile;

    public LicenseHeaderInfo(
            String licenseName,
            Map<String, String> properties,
            Path licenseTemplateFile) {
        ExceptionHelper.checkNotNullArgument(licenseName, "licenseName");
        ExceptionHelper.checkNotNullArgument(properties, "properties");

        this.licenseName = licenseName;
        this.properties = CollectionUtils.copyNullSafeHashMap(properties);
        this.licenseTemplateFile = licenseTemplateFile;
    }

    public Path getLicenseTemplateFile() {
        return licenseTemplateFile;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
