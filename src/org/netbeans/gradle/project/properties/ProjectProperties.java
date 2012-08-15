package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import org.netbeans.api.java.platform.JavaPlatform;

public final class ProjectProperties {
    private final MutableProperty<JavaPlatform> platform;
    private final MutableProperty<Charset> sourceEncoding;

    public ProjectProperties() {
        this.platform = new DefaultMutableProperty<JavaPlatform>(JavaPlatform.getDefault(), false);
        this.sourceEncoding = new DefaultMutableProperty<Charset>(Charset.forName("UTF-8"), false);
    }

    public MutableProperty<JavaPlatform> getPlatform() {
        return platform;
    }

    public MutableProperty<Charset> getSourceEncoding() {
        return sourceEncoding;
    }
}
