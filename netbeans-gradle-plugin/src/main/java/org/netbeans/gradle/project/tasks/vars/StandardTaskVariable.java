package org.netbeans.gradle.project.tasks.vars;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.java.test.SpecificTestClass;
import org.netbeans.gradle.project.java.test.SpecificTestcase;
import org.netbeans.gradle.project.java.test.TestTaskName;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.ValueGetter;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableDef;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableValue;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

public enum StandardTaskVariable {
    PROJECT_PATH_NOT_NORMALIZED("project-path", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            String uniqueName = project.currentModel().getValue().getMainProject().getProjectFullName();
            return new VariableValue(uniqueName);
        }
    }),
    PROJECT_PATH_NORMALIZED("project", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            String uniqueName = project.currentModel().getValue().getMainProject().getProjectFullName();
            if (":".equals(uniqueName)) { // This is the root project.
                uniqueName = "";
            }
            return new VariableValue(uniqueName);
        }
    }),
    SELECTED_CLASS("selected-class", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            SpecificTestClass testClass = actionContext.lookup(SpecificTestClass.class);
            if (testClass != null) {
                return new VariableValue(testClass.getTestClassName());
            }

            FileObject file = getFileOfContext(actionContext);
            if (file == null) {
                return VariableValue.NULL_VALUE;
            }

            return getClassNameForFile(project, file);
        }
    }),
    SELECTED_FILE("selected-file", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            FileObject fileObject = getFileOfContext(actionContext);
            if (fileObject == null) {
                return VariableValue.NULL_VALUE;
            }

            File file = FileUtil.toFile(fileObject);
            if (file == null) {
                return VariableValue.NULL_VALUE;
            }

            return new VariableValue(file.getPath());
        }
    }),
    TEST_FILE_PATH("test-file-path", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            String selectedClass = variables.tryGetValueForVariable(SELECTED_CLASS.getVariable());
            return new VariableValue(deduceFromClass(selectedClass));
        }

        private String deduceFromClass(String selectedClass) {
            return selectedClass != null
                    ? selectedClass.replace('.', '/')
                    : null;
        }
    }),
    TEST_METHOD("test-method", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            return getMethodReplaceVariable(variables, project, actionContext);
        }
    }),
    PLATFORM_DIR("platform-dir", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            ProjectPlatform targetPlatform = project.getCommonProperties().targetPlatform().getActiveValue();
            FileObject rootFolder = targetPlatform != null ? targetPlatform.getRootFolder() : null;
            return new VariableValue(rootFolder != null
                    ? FileUtil.getFileDisplayName(rootFolder)
                    : null);
        }
    }),
    TEST_TASK_NAME("test-task-name", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            return new VariableValue(TestTaskName.getTaskName(actionContext));
        }
    }),
    TEST_TASK_NAME_CAPITAL("test-task-name-capital", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            String value = variables.tryGetValueForVariable(TEST_TASK_NAME.getVariable());
            return new VariableValue(value != null
                    ? StringUtils.capitalizeFirstCharacter(value)
                    : null);
        }
    }),
    CMD_LINE_ARGS("cmd-line-args", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            return VariableValue.EMPTY_VALUE;
        }
    }),
    JVM_LINE_ARGS("jvm-line-args", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            return VariableValue.EMPTY_VALUE;
        }
    });

    private static final Logger LOGGER = Logger.getLogger(StandardTaskVariable.class.getName());
    private static final CachingVariableMap.VariableDefMap<NbGradleProject> TASK_VARIABLE_MAP
            = createStandardMap();

    private static VariableValue getClassNameForFile(NbGradleProject project, FileObject file) {
        SourceGroup[] sourceGroups = ProjectUtils.getSources(project)
                .getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);

        String relFileName = null;
        for (SourceGroup group: sourceGroups) {
            FileObject sourceRoot = group.getRootFolder();
            String relPath = FileUtil.getRelativePath(sourceRoot, file);
            if (relPath != null) {
                // Remove the ".java" or ".groovy" from the end of
                // the file name
                relFileName = removeExtension(relPath);
                break;
            }
        }

        return new VariableValue(relFileName != null ? relFileName.replace('/', '.') : null);
    }

    private static VariableValue getMethodReplaceVariable(
            TaskVariableMap variables,
            NbGradleProject project,
            SingleMethod method) {

        String selectedClass = variables.tryGetValueForVariable(SELECTED_CLASS.getVariable());
        if (selectedClass == null) {
            selectedClass = getClassNameForFile(project, method.getFile()).value;
            if (selectedClass == null) {
                LOGGER.log(Level.INFO, "Could not find class file name for file {0}", method.getFile());
                return VariableValue.NULL_VALUE;
            }
        }

        return new VariableValue(selectedClass + "." + method.getMethodName());
    }

    private static VariableValue getMethodReplaceVariable(
            TaskVariableMap variables,
            NbGradleProject project,
            Lookup actionContext) {

        SingleMethod method = actionContext.lookup(SingleMethod.class);
        if (method != null) {
            return getMethodReplaceVariable(variables, project, method);
        }

        SpecificTestcase specificTestcase = actionContext.lookup(SpecificTestcase.class);
        if (specificTestcase != null) {
            return new VariableValue(specificTestcase.getTestIncludePattern());
        }

        return VariableValue.NULL_VALUE;
    }

    private static String removeExtension(String filePath) {
        int extSeparatorIndex = filePath.lastIndexOf('.');
        return extSeparatorIndex >= 0
                ? filePath.substring(0, extSeparatorIndex)
                : filePath;
    }

    private static List<FileObject> getFilesOfContext(Lookup context) {
        List<FileObject> files = new ArrayList<>();
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

    public static TaskVariableMap createVarReplaceMap(
            NbGradleProject project, Lookup actionContext) {
        return new CachingVariableMap<>(TASK_VARIABLE_MAP, project, actionContext);
    }

    private final TaskVariable variable;
    private final ValueGetter<NbGradleProject> valueGetter;

    private StandardTaskVariable(String variableName, ValueGetter<NbGradleProject> valueGetter) {
        this.variable = new TaskVariable(variableName);
        this.valueGetter = valueGetter;
    }

    public TaskVariable getVariable() {
        return variable;
    }

    public String getVariableName() {
        return variable.getVariableName();
    }

    public String getScriptReplaceConstant() {
        return variable.getScriptReplaceConstant();
    }

    private static CachingVariableMap.VariableDefMap<NbGradleProject> createStandardMap() {
        StandardTaskVariable[] variables = StandardTaskVariable.values();

        final Map<TaskVariable, CachingVariableMap.VariableDef<NbGradleProject>> result
                = CollectionUtils.newHashMap(variables.length);

        for (StandardTaskVariable variable: variables) {
            result.put(variable.getVariable(), variable.asVariableDef());
        }

        return new CachingVariableMap.VariableDefMap<NbGradleProject>() {
            @Override
            public CachingVariableMap.VariableDef<NbGradleProject> tryGetDef(TaskVariable variable) {
                return result.get(variable);
            }
        };
    }

    private VariableDef<NbGradleProject> asVariableDef() {
        return new VariableDef<>(variable, valueGetter);
    }
}
