package org.netbeans.gradle.model.internal;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class ModelQueryOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BasicInfo basicInfo;

    // Keys -> List of results of ProjectInfoBuilder
    private final CustomSerializedMap projectInfoResults;
    private final Throwable issue;

    public ModelQueryOutput(
            BasicInfo basicInfo,
            CustomSerializedMap projectInfoResults,
            Throwable issue) {
        if (basicInfo == null) throw new NullPointerException("basicInfo");
        if (projectInfoResults == null) throw new NullPointerException("projectInfoResults");

        this.basicInfo = basicInfo;
        this.projectInfoResults = projectInfoResults;
        this.issue = TransferableExceptionWrapper.wrap(issue);
    }

    public Throwable getIssue() {
        return issue;
    }

    public BasicInfo getBasicInfo() {
        return basicInfo;
    }

    public CustomSerializedMap getProjectInfoResults() {
        return projectInfoResults;
    }

    public static final class BasicInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private final ProjectId projectId;
        private final String projectFullName;
        private final File buildScript;
        private final File buildDir;
        private final Collection<GradleTaskID> tasks;

        public BasicInfo(
                ProjectId projectId,
                String projectFullName,
                File buildScript,
                File buildDir,
                Collection<GradleTaskID> tasks) {
            if (projectId == null) throw new NullPointerException("projectId");
            if (projectFullName == null) throw new NullPointerException("projectFullName");
            if (buildDir == null) throw new NullPointerException("buildDir");

            this.projectId = projectId;
            this.projectFullName = projectFullName;
            this.buildScript = buildScript;
            this.buildDir = buildDir;
            this.tasks = CollectionUtils.copyNullSafeList(tasks);
        }

        public ProjectId getProjectId() {
            return projectId;
        }

        public String getProjectFullName() {
            return projectFullName;
        }

        public File getBuildScript() {
            return buildScript;
        }

        public File getBuildDir() {
            return buildDir;
        }

        public Collection<GradleTaskID> getTasks() {
            return tasks;
        }
    }
}
