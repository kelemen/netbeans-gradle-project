package org.netbeans.gradle.project.model.issue;

import java.util.Objects;
import javax.annotation.Nullable;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;

public final class ModelLoadIssue {
    private final NbGradleProject requestedProject;
    private final String actualProjectPath;
    private final NbGradleExtensionRef extensionRef;
    private final String builderName;
    private final Throwable stackTrace;

    public ModelLoadIssue(
            NbGradleProject requestedProject,
            String actualProjectPath,
            NbGradleExtensionRef extensionRef,
            String builderName,
            Throwable stackTrace) {

        this.requestedProject = Objects.requireNonNull(requestedProject, "requestedProject");
        this.actualProjectPath = actualProjectPath;
        this.extensionRef = extensionRef;
        this.builderName = builderName;
        this.stackTrace = Objects.requireNonNull(stackTrace, "stackTrace");
    }

    public NbGradleProject getRequestedProject() {
        return requestedProject;
    }

    @Nullable
    public String getActualProjectPath() {
        return actualProjectPath;
    }

    @Nullable
    public NbGradleExtensionRef getExtensionRef() {
        return extensionRef;
    }

    @Nullable
    public String getExtensionName() {
        return extensionRef != null ? extensionRef.getName() : null;
    }

    @Nullable
    public String getBuilderName() {
        return builderName;
    }

    public Throwable getStackTrace() {
        return stackTrace;
    }

    @Override
    public String toString() {
        return "ModelLoadIssue{"
                + "requestedProject=" + requestedProject
                + ", actualProjectPath=" + actualProjectPath
                + ", extensionName=" + getExtensionName() + '}';
    }
}
