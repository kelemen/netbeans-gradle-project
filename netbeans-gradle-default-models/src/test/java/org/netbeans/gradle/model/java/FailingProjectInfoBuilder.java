package org.netbeans.gradle.model.java;

import org.gradle.api.Project;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.BuilderUtils;

public final class FailingProjectInfoBuilder implements ProjectInfoBuilder<Void> {
    private static final long serialVersionUID = 1L;

    private final String exceptionMessage;

    public FailingProjectInfoBuilder(String exceptionMessage) {
        if (exceptionMessage == null) throw new NullPointerException("exceptionMessage");
        this.exceptionMessage = exceptionMessage;
    }

    public Void getProjectInfo(Project project) {
        throw new NotSerializableException(exceptionMessage);
    }

    public String getName() {
        return BuilderUtils.getNameForGenericBuilder(this, exceptionMessage);
    }

    @SuppressWarnings("serial")
    private static final class NotSerializableException extends RuntimeException {
        public final Object blockerOfSerialization;

        public NotSerializableException(String message) {
            super(message);
            blockerOfSerialization = new Object();
        }
    }
}
