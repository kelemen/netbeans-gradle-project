package org.netbeans.gradle.project.properties;

import java.io.File;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;

public final class LicenseHeaderInfo {
    private static final Random RND = new SecureRandom();

    private final String licenseName;
    private final String privateName;
    private final Map<String, String> properties;
    private final Path licenseTemplateFile;

    public LicenseHeaderInfo(
            String licenseName,
            Map<String, String> properties,
            Path licenseTemplateFile) {
        ExceptionHelper.checkNotNullArgument(licenseName, "licenseName");
        ExceptionHelper.checkNotNullArgument(properties, "properties");

        this.licenseName = licenseName;
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
        this.licenseTemplateFile = licenseTemplateFile;

        String randomStr = Long.toHexString(RND.nextLong()) + "-" + Long.toHexString(RND.nextLong());
        this.privateName = "nb-gradle-" + safeLicenseName(licenseName) + "-" + randomStr;
    }

    public Path getLicenseTemplateFile() {
        return licenseTemplateFile;
    }

    public Path getLicenseTemplateFile(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        if (licenseTemplateFile == null) {
            return null;
        }

        if (licenseTemplateFile.isAbsolute()) {
            return licenseTemplateFile;
        }

        File rootProjectDir = project.currentModel().getValue().getRootProjectDir();
        return rootProjectDir.toPath().resolve(licenseTemplateFile);
    }

    private static boolean isSafeChar(char ch) {
        if (ch >= 'A' && ch <= 'Z') return true;
        if (ch >= 'a' && ch <= 'z') return true;
        if (ch >= '0' && ch <= '9') return true;

        return "_-$. ".indexOf(ch) >= 0;
    }

    // Safe to be used as a filename
    private static String safeLicenseName(String name) {
        StringBuilder result = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            result.append(isSafeChar(ch) ? ch : "_");
        }
        return result.toString();
    }

    public String getPrivateLicenseName(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        if (licenseTemplateFile != null) {
            return privateName;
        }
        else {
            return licenseName;
        }
    }

    public String getLicenseName() {
        return licenseName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
