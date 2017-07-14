package org.netbeans.gradle.project.java.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.utils.LazyValues;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.api.task.GradleTaskVariableQuery;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.test.TestTaskName;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.ValueGetter;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableDef;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableDefMap;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableValue;
import org.netbeans.gradle.project.tasks.vars.StandardTaskVariable;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

public final class JavaGradleTaskVariableQuery implements GradleTaskVariableQuery {
    public static final TaskVariable SOURCE_SET_NAME = new TaskVariable("source-set-name");
    public static final TaskVariable SOURCE_SET_NAME_CAPITAL = new TaskVariable("source-set-name-capital");

    public static final TaskVariable TEST_TASK_NAME = StandardTaskVariable.TEST_TASK_NAME.getVariable();
    public static final TaskVariable TEST_TASK_NAME_CAPITAL = StandardTaskVariable.TEST_TASK_NAME_CAPITAL.getVariable();

    public static final TaskVariable COVERAGE_REPORT_TASK_NAME = new TaskVariable("coverage-report-task");

    private static final Supplier<VariableDefMap<JavaExtension>> VARIABLE_DEF_MAP_REF
            = LazyValues.lazyValue(JavaGradleTaskVariableQuery::createVariableDefMap);

    private final JavaExtension javaExt;

    public JavaGradleTaskVariableQuery(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
    }

    private static String getSourceSetName(JavaExtension project, Lookup actionContext) {
        // TODO: Define a special object on the lookup to be found, this will
        //       allow us to explicitly define the source sets for actions.

        FileObject fileOfContextObj = getFileOfContext(actionContext);
        if (fileOfContextObj == null) {
            return null;
        }

        File fileOfContext = FileUtil.toFile(fileOfContextObj);
        if (fileOfContext == null) {
            return null;
        }

        List<JavaSourceSet> sourceSets = project.getCurrentModel().getMainModule().getSources();
        for (JavaSourceSet sourceSet: sourceSets) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                for (File sourceRoot: sourceGroup.getSourceRoots()) {
                    if (NbFileUtils.isParentOrSame(sourceRoot, fileOfContext)) {
                        return sourceSet.getName();
                    }
                }
            }
        }
        return null;
    }

    private static String getTestTaskName(TaskVariableMap variables, JavaExtension project, Lookup actionContext) {
        String taskName = TestTaskName.tryGetTaskName(actionContext);
        if (taskName != null) {
            return taskName;
        }

        taskName = variables.tryGetValueForVariable(SOURCE_SET_NAME);
        if (taskName != null) {
            return project.getCurrentModel().getMainModule().findTestTaskForSourceSet(taskName);
        }
        return TestTaskName.DEFAULT_TEST_TASK_NAME;
    }

    private static VariableValue getCapitalized(TaskVariableMap variables, TaskVariable taskVariable) {
        String value = variables.tryGetValueForVariable(taskVariable);
        return value != null
                ? new VariableValue(StringUtils.capitalizeFirstCharacter(value))
                : VariableValue.NULL_VALUE;
    }

    private static void defineVariables(Map<TaskVariable, VariableDef<JavaExtension>> varMap) {
        addVariable(varMap, SOURCE_SET_NAME, (TaskVariableMap variables, JavaExtension project, Lookup actionContext) -> {
            return new VariableValue(getSourceSetName(project, actionContext));
        });

        addVariable(varMap, SOURCE_SET_NAME_CAPITAL, (TaskVariableMap variables, JavaExtension project, Lookup actionContext) -> {
            return getCapitalized(variables, SOURCE_SET_NAME);
        });

        addVariable(varMap, TEST_TASK_NAME, (TaskVariableMap variables, JavaExtension project, Lookup actionContext) -> {
            return new VariableValue(getTestTaskName(variables, project, actionContext));
        });

        addVariable(varMap, TEST_TASK_NAME_CAPITAL, (TaskVariableMap variables, JavaExtension project, Lookup actionContext) -> {
            return getCapitalized(variables, TEST_TASK_NAME);
        });

        addVariable(varMap, COVERAGE_REPORT_TASK_NAME, (TaskVariableMap variables, JavaExtension project, Lookup actionContext) -> {
            boolean hasCodeCoverage = project.getCurrentModel().getMainModule().getCodeCoverage().hasCodeCoverage();
            // TODO: It should be somehow determined by the build script
            return new VariableValue(hasCodeCoverage ? "jacocoTestReport" : "");
        });
    }

    private static VariableDefMap<JavaExtension> getVariableDefMap() {
        return VARIABLE_DEF_MAP_REF.get();
    }

    private static VariableDefMap<JavaExtension> createVariableDefMap() {
        Map<TaskVariable, VariableDef<JavaExtension>> varMap = new HashMap<>();
        defineVariables(varMap);
        return varMap::get;
    }

    private static void addVariable(
            Map<TaskVariable, VariableDef<JavaExtension>> varMap,
            TaskVariable taskVariable,
            ValueGetter<JavaExtension> valueGetter) {
        varMap.put(taskVariable, new VariableDef<>(taskVariable, valueGetter));
    }

    @Override
    public TaskVariableMap getVariableMap(final Lookup actionContext) {
        return new CachingVariableMap<>(getVariableDefMap(), javaExt, actionContext);
    }

    private static List<FileObject> getFilesOfContext(Lookup context) {
        List<FileObject> files = new ArrayList<>();

        SingleMethod method = context.lookup(SingleMethod.class);
        if (method != null) {
            files.add(method.getFile());
        }

        for (DataObject dataObj: context.lookupAll(DataObject.class)) {
            FileObject file = dataObj.getPrimaryFile();
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }

    private static FileObject getFileOfContext(Lookup context) {
        List<FileObject> files = getFilesOfContext(context);
        if (files.isEmpty()) {
            return null;
        }

        FileObject file = files.get(0);
        if (file == null) {
            return null;
        }

        return file.isFolder() ? null : file;
    }
}
