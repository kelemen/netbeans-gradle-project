package org.netbeans.gradle.model.java;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.tasks.JacocoReportsContainer;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.BuilderUtils;

public enum JacocoModelBuilder implements ProjectInfoBuilder<JacocoModel> {
    INSTANCE;

    public JacocoModel getProjectInfo(Project project) {
        if (!project.getPlugins().hasPlugin(JacocoPlugin.class)) {
            return null;
        }

        TaskCollection<? extends Task> reportTasks = project.getTasks().withType(JacocoReport.class);
        if (reportTasks.size() <= 0) {
            return null;
        }

        JacocoReportFiles reportFiles = tryGetReportFiles(reportTasks.iterator().next());
        if (reportFiles == null) {
            return null;
        }

        return new JacocoModel(reportFiles);
    }

    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }

    private static JacocoReportFiles tryGetReportFiles(Task task) {
        if (!(task instanceof JacocoReport)) {
            return null;
        }

        JacocoReport reportTask = (JacocoReport)task;
        JacocoReportsContainer reports = reportTask.getReports();

        return new JacocoReportFiles(
                reports.getHtml().getEntryPoint(),
                reports.getXml().getDestination());
    }
}
