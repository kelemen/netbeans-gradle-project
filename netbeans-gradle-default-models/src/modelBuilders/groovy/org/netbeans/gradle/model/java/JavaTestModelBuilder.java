package org.netbeans.gradle.model.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.testing.JUnitXmlReport;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestTaskReports;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.BuilderUtils;

enum JavaTestModelBuilder
implements
        ProjectInfoBuilder2<JavaTestModel> {
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(JavaTestModelBuilder.class.getName());

    @Override
    public JavaTestModel getProjectInfo(Object project) {
        return getProjectInfo((Project)project);
    }

    private JavaTestModel getProjectInfo(Project project) {
        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            return null;
        }

        TaskCollection<? extends Task> allTests = project.getTasks().withType(Test.class);
        List<JavaTestTask> result = new ArrayList<JavaTestTask>(allTests.size());
        XmlOutputDirGetter outputDirGetter = new XmlOutputDirGetter(project);

        for (Task task: allTests) {
            File xmlOutputDir = outputDirGetter.getXmlOutputDir(task);
            JavaTestTask taskInfo = new JavaTestTask(task.getName(), xmlOutputDir);
            result.add(taskInfo);
        }

        return new JavaTestModel(result);
    }

    /** {@inheritDoc } */
    @Override
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }

    private static final class XmlOutputDirGetter {
        private final Project project;

        private XmlOutputDirGetter(Project project) {
            this.project = project;
        }

        private File tryGetXmlOutputDir(Task task) {
            Object reportsObj = task.property("reports");
            if (!(reportsObj instanceof TestTaskReports)) {
                return null;
            }

            TestTaskReports reports = (TestTaskReports)reportsObj;

            try {
                JUnitXmlReport junitXml = reports.getJunitXml();
                return junitXml != null ? junitXml.getDestination() : null;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Cannot get XML report file for test reports", ex);
                return null;
            }
        }

        public File getXmlOutputDir(Task task) {
            File result = tryGetXmlOutputDir(task);
            return result != null
                    ? result
                    : getDefaultXmlOutput(project);
        }

        private static File getDefaultXmlOutput(Project project) {
            return new File(project.getBuildDir(), "test-results");
        }
    }
}
