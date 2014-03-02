package org.netbeans.gradle.project.properties;

import java.io.File;
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
    private final File licenseTemplateFile;

    public LicenseHeaderInfo(
            String licenseName,
            Map<String, String> properties,
            File licenseTemplateFile) {
        ExceptionHelper.checkNotNullArgument(licenseName, "licenseName");
        ExceptionHelper.checkNotNullArgument(properties, "properties");

        this.licenseName = licenseName;
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
        this.licenseTemplateFile = licenseTemplateFile;

        String randomStr = Long.toHexString(RND.nextLong()) + "-" + Long.toHexString(RND.nextLong());
        this.privateName = "nb-gradle-" + safeLicenseName(licenseName) + "-" + randomStr;
    }

    public File getLicenseTemplateFile() {
        return licenseTemplateFile;
    }

    public File getLicenseTemplateFile(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        if (licenseTemplateFile == null) {
            return null;
        }

        if (licenseTemplateFile.isAbsolute()) {
            return licenseTemplateFile;
        }

        File rootProjectDir = project.getAvailableModel().getRootProjectDir();
        return new File(rootProjectDir, licenseTemplateFile.getPath());
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
