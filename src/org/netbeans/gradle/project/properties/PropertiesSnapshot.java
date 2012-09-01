package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.CollectionUtils;

public final class PropertiesSnapshot {
    public static final class Builder {
        private String sourceLevel;
        private JavaPlatform platform;
        private Charset sourceEncoding;
        private List<PredefinedTask> commonTasks;

        public Builder() {
            this.platform = JavaPlatform.getDefault();
            if (this.platform == null) {
                throw new IllegalStateException("There is no default JDK.");
            }
            this.sourceEncoding = AbstractProjectProperties.DEFAULT_SOURCE_ENCODING;
            this.sourceLevel = AbstractProjectProperties.getSourceLevelFromPlatform(this.platform);
            this.commonTasks = Collections.emptyList();
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

        public List<PredefinedTask> getCommonTasks() {
            return commonTasks;
        }

        public void setCommonTasks(List<PredefinedTask> commonTasks) {
            this.commonTasks = CollectionUtils.copyNullSafeList(commonTasks);
        }

        public PropertiesSnapshot create() {
            return new PropertiesSnapshot(this);
        }
    }

    private final String sourceLevel;
    private final JavaPlatform platform;
    private final Charset sourceEncoding;
    private final List<PredefinedTask> commonTasks;

    public PropertiesSnapshot(ProjectProperties properties) {
        this.sourceLevel = properties.getSourceLevel().getValue();
        this.platform = properties.getPlatform().getValue();
        this.sourceEncoding = properties.getSourceEncoding().getValue();
        this.commonTasks = properties.getCommonTasks().getValue();

        if (sourceLevel == null) throw new NullPointerException("sourceLevel");
        if (platform == null) throw new NullPointerException("platform");
        if (sourceEncoding == null) throw new NullPointerException("sourceEncoding");
        for (PredefinedTask task: this.commonTasks) {
            if (task == null) throw new NullPointerException("task");
        }
    }

    private PropertiesSnapshot(Builder builder) {
        this.sourceLevel = builder.getSourceLevel();
        this.platform = builder.getPlatform();
        this.sourceEncoding = builder.getSourceEncoding();
        this.commonTasks = builder.getCommonTasks();
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

    public List<PredefinedTask> getCommonTasks() {
        return commonTasks;
    }
}
