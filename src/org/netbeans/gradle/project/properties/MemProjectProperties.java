package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import org.netbeans.api.java.platform.JavaPlatform;

public final class MemProjectProperties extends AbstractProjectProperties {
    private final MutableProperty<String> sourceLevel;
    private final MutableProperty<JavaPlatform> platform;
    private final MutableProperty<Charset> sourceEncoding;

    public MemProjectProperties() {
        JavaPlatform defaultPlatform = JavaPlatform.getDefault();
        this.sourceLevel = new DefaultMutableProperty<String>(getSourceLevelFromPlatform(defaultPlatform), false);
        this.platform = new DefaultMutableProperty<JavaPlatform>(defaultPlatform, false);
        this.sourceEncoding = new DefaultMutableProperty<Charset>(DEFAULT_SOURCE_ENCODING, false);
    }

    @Override
    public MutableProperty<String> getSourceLevel() {
        return sourceLevel;
    }

    @Override
    public MutableProperty<JavaPlatform> getPlatform() {
        return platform;
    }

    @Override
    public MutableProperty<Charset> getSourceEncoding() {
        return sourceEncoding;
    }
}
