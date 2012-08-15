package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.regex.Pattern;
import org.netbeans.api.java.platform.JavaPlatform;
import org.openide.modules.SpecificationVersion;

public final class ProjectProperties {
    public static final Charset DEFAULT_SOURCE_ENCODING = Charset.forName("UTF-8");

    private final MutableProperty<String> sourceLevel;
    private final MutableProperty<JavaPlatform> platform;
    private final MutableProperty<Charset> sourceEncoding;

    public ProjectProperties() {
        JavaPlatform defaultPlatform = JavaPlatform.getDefault();
        this.sourceLevel = new DefaultMutableProperty<String>(getSourceLevelFromPlatform(defaultPlatform), false);
        this.platform = new DefaultMutableProperty<JavaPlatform>(defaultPlatform, false);
        this.sourceEncoding = new DefaultMutableProperty<Charset>(DEFAULT_SOURCE_ENCODING, false);
    }

    public static String getSourceLevelFromPlatform(JavaPlatform platform) {
        SpecificationVersion version = platform.getSpecification().getVersion();
        String[] versionParts = version.toString().split(Pattern.quote("."));
        if (versionParts.length < 2) {
            return "1.7";
        }
        else {
            return versionParts[0] + "." + versionParts[1];
        }
    }

    public MutableProperty<String> getSourceLevel() {
        return sourceLevel;
    }

    public MutableProperty<JavaPlatform> getPlatform() {
        return platform;
    }

    public MutableProperty<Charset> getSourceEncoding() {
        return sourceEncoding;
    }
}
