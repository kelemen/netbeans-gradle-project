package org.netbeans.gradle.project.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class LicenseHeaderInfo {
    private final String licenseName;
    private final Map<String, String> properties;

    public LicenseHeaderInfo(String licenseName, Map<String, String> properties) {
        if (licenseName == null) throw new NullPointerException("licenseName");

        this.licenseName = licenseName;
        this.properties = Collections.unmodifiableMap(new HashMap<String, String>(properties));
    }

    public String getLicenseName() {
        return licenseName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
