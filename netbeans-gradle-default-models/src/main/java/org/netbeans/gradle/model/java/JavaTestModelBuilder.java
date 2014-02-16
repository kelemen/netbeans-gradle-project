package org.netbeans.gradle.model.java;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskCollection;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.gradleclasses.GradleClass;
import org.netbeans.gradle.model.gradleclasses.GradleClasses;
import org.netbeans.gradle.model.util.BuilderUtils;

public enum JavaTestModelBuilder
implements
        ProjectInfoBuilder<JavaTestModel> {
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(JavaTestModelBuilder.class.getName());

    public JavaTestModel getProjectInfo(Project project) {
        if (!project.getPlugins().hasPlugin("java")) {
            return null;
        }

        Class<? extends Task> testClass = GradleClasses.tryGetGradleClass(
                project, "org.gradle.api.tasks.testing.Test", Task.class);
        if (testClass == null) {
            LOGGER.warning("Cannot find class of Test tasks.");
            return JavaTestModel.getDefaulTestModel(project.getProjectDir());
        }

        TaskCollection<? extends Task> allTests = project.getTasks().withType(testClass);
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
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }

    private static final class XmlOutputDirGetter {
        private final Project project;
        private final GradleClass junitXmlReportClass;

        private XmlOutputDirGetter(Project project) {
            this.project = project;
            this.junitXmlReportClass = tryGetJUnitXmlReportClass(project);
        }

        @SuppressWarnings("UseSpecificCatch")
        private File tryGetXmlOutputDir(Task task) {
            if (junitXmlReportClass == null) {
                return null;
            }

            Object reports = task.property("reports");
            if (!junitXmlReportClass.getType().isInstance(reports)) {
                return null;
            }

            try {
                Method getDestination = junitXmlReportClass.getMethod("getDestination");
                Object destination = getDestination.invoke(reports);
                return destination instanceof File
                        ? (File)destination
                        : null;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Cannot call the getDestination() method of JUnitXmlReport.", ex);
                return null;
            }
        }

        public File getXmlOutputDir(Task task) {
            File result = tryGetXmlOutputDir(task);
            return result != null
                    ? result
                    : getDefaultXmlOutput(project, task);
        }

        private static File getDefaultXmlOutput(Project project, Task task) {
            String name = task.getName();
            return new File(project.getBuildDir(), name + "-results");
        }

        private static GradleClass tryGetJUnitXmlReportClass(Project project) {
            try {
                return GradleClasses.getGradleClass(project, "org.gradle.api.tasks.testing.JUnitXmlReport");
            } catch (ClassNotFoundException ex) {
                LOGGER.warning("Missing class: JUnitXmlReport.");
                return null;
            }
        }
    }
}
