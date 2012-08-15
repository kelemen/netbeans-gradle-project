package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import org.netbeans.api.java.platform.JavaPlatform;

public final class ProjectProperties {
    public static final Charset DEFAULT_SOURCE_ENCODING = Charset.forName("UTF-8");

    private final MutableProperty<JavaPlatform> platform;
    private final MutableProperty<Charset> sourceEncoding;

    public ProjectProperties() {
        this.platform = new DefaultMutableProperty<JavaPlatform>(JavaPlatform.getDefault(), false);
        this.sourceEncoding = new DefaultMutableProperty<Charset>(DEFAULT_SOURCE_ENCODING, false);
    }

    public MutableProperty<JavaPlatform> getPlatform() {
        return platform;
    }

    public MutableProperty<Charset> getSourceEncoding() {
        return sourceEncoding;
    }
}
