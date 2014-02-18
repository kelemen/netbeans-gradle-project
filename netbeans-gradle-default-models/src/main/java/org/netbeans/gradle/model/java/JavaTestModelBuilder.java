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
        private final GradleClass testTaskReportsClass;

        private XmlOutputDirGetter(Project project) {
            this.project = project;
            this.junitXmlReportClass = tryGetJUnitXmlReportClass(project);
            this.testTaskReportsClass = tryGetTestTaskReportsClass(project);
        }

        @SuppressWarnings("UseSpecificCatch")
        private static <T> T tryCallGetter(
                Object instance,
                GradleClass type,
                String methodName,
                Class<T> resultType) {

            try {
                Method method = type.getMethod(methodName);
                Object result = method.invoke(instance);
                return resultType.isInstance(result)
                        ? resultType.cast(result)
                        : null;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING,
                        "Cannot call the " + methodName + " method of " + type.getType().getName(),
                        ex);
                return null;
            }
        }

        private File tryGetXmlOutputDir(Task task) {
            if (junitXmlReportClass == null || testTaskReportsClass == null) {
                return null;
            }

            Object reports = task.property("reports");
            if (!testTaskReportsClass.getType().isInstance(reports)) {
                return null;
            }

            Object junitXml = tryCallGetter(
                    reports,
                    testTaskReportsClass,
                    "getJunitXml",
                    junitXmlReportClass.getType());
            return tryCallGetter(junitXml, junitXmlReportClass, "getDestination", File.class);
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

        private static GradleClass tryGetClass(Project project, String className) {
            try {
                return GradleClasses.getGradleClass(project, className);
            } catch (ClassNotFoundException ex) {
                LOGGER.log(Level.WARNING, "Missing class: {0}", className);
                return null;
            }
        }

        private static GradleClass tryGetJUnitXmlReportClass(Project project) {
            return tryGetClass(project, "org.gradle.api.tasks.testing.JUnitXmlReport");
        }

        private static GradleClass tryGetTestTaskReportsClass(Project project) {
            return tryGetClass(project, "org.gradle.api.tasks.testing.TestTaskReports");
        }
    }
}
