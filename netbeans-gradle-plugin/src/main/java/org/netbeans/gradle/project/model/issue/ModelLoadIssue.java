package org.netbeans.gradle.project.model.issue;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public interface ModelLoadIssue {
    public enum Severity {
        INTERNAL_ERROR(100),
        EXTENSION_ERROR(50),
        BUILD_SCRIPT_ERROR(30);

        private final int value;

        private Severity(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Nonnull
    public Severity getSeverity();

    @Nonnull
    public String getIssueDescription();

    @CheckForNull
    public Throwable getStackTrace();
}
