package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import org.netbeans.api.java.platform.JavaPlatform;

public final class PropertiesSnapshot {
    public static final class Builder {
        private String sourceLevel;
        private JavaPlatform platform;
        private Charset sourceEncoding;

        public Builder() {
            this.platform = JavaPlatform.getDefault();
            if (this.platform == null) {
                throw new IllegalStateException("There is no default JDK.");
            }
            this.sourceEncoding = AbstractProjectProperties.DEFAULT_SOURCE_ENCODING;
            this.sourceLevel = AbstractProjectProperties.getSourceLevelFromPlatform(this.platform);
        }

        public String getSourceLevel() {
            return sourceLevel;
        }

        public void setSourceLevel(String sourceLevel) {
            if (sourceLevel == null) throw new NullPointerException("sourceLevel");
            this.sourceLevel = sourceLevel;
        }

        public JavaPlatform getPlatform() {
            return platform;
        }

        public void setPlatform(JavaPlatform platform) {
            if (platform == null) throw new NullPointerException("platform");
            this.platform = platform;
        }

        public Charset getSourceEncoding() {
            return sourceEncoding;
        }

        public void setSourceEncoding(Charset sourceEncoding) {
            if (sourceEncoding == null) throw new NullPointerException("sourceEncoding");
            this.sourceEncoding = sourceEncoding;
        }

        public PropertiesSnapshot create() {
            return new PropertiesSnapshot(this);
        }
    }

    private final String sourceLevel;
    private final JavaPlatform platform;
    private final Charset sourceEncoding;

    public PropertiesSnapshot(ProjectProperties properties) {
        this.sourceLevel = properties.getSourceLevel().getValue();
        this.platform = properties.getPlatform().getValue();
        this.sourceEncoding = properties.getSourceEncoding().getValue();

        if (sourceLevel == null) throw new NullPointerException("sourceLevel");
        if (platform == null) throw new NullPointerException("platform");
        if (sourceEncoding == null) throw new NullPointerException("sourceEncoding");
    }

    private PropertiesSnapshot(Builder builder) {
        this.sourceLevel = builder.getSourceLevel();
        this.platform = builder.getPlatform();
        this.sourceEncoding = builder.getSourceEncoding();
    }

    public String getSourceLevel() {
        return sourceLevel;
    }

    public JavaPlatform getPlatform() {
        return platform;
    }

    public Charset getSourceEncoding() {
        return sourceEncoding;
    }
}
