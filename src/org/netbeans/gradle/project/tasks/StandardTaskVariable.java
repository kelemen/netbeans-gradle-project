package org.netbeans.gradle.project.tasks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

public enum StandardTaskVariable {
    PROJECT_NAME("project", new ValueGetter() {
        @Override
        public VariableValue tryGetValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            String uniqueName = project.getAvailableModel().getMainModule().getUniqueName();
            if (":".equals(uniqueName)) { // This is the root project.
                uniqueName = "";
            }
            return new VariableValue(uniqueName);
        }
    }),
    SELECTED_CLASS("selected-class", new ValueGetter() {
        @Override
        public VariableValue tryGetValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            FileObject file = getFileOfContext(actionContext);
            if (file == null) {
                return null;
            }

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
    }),
    TEST_FILE_PATH("test-file-path", new ValueGetter() {
        @Override
        public VariableValue tryGetValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            String selectedClass = variables.tryGetValueForVariable(SELECTED_CLASS.getVariable());
            return new VariableValue(deduceFromClass(selectedClass));
        }

        private String deduceFromClass(String selectedClass) {
            return selectedClass != null
                    ? selectedClass.replace('.', '/')
                    : null;
        }
    }),
    PLATFORM_DIR("platform-dir", new ValueGetter() {
        @Override
        public VariableValue tryGetValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            FileObject rootFolder = project.getProperties().getPlatform().getValue().getRootFolder();
            return new VariableValue(rootFolder != null
                    ? FileUtil.getFileDisplayName(rootFolder)
                    : null);
        }
    });

    private static final Map<TaskVariable, StandardTaskVariable> TASK_VARIABLE_MAP
            = createStandardMap();

    private static String removeExtension(String filePath) {
        int extSeparatorIndex = filePath.lastIndexOf('.');
        return extSeparatorIndex >= 0
                ? filePath.substring(0, extSeparatorIndex)
                : filePath;
    }

    private static List<FileObject> getFilesOfContext(Lookup context) {
        List<FileObject> files = new LinkedList<FileObject>();
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

    private static Map<TaskVariable, StandardTaskVariable> createStandardMap() {
        StandardTaskVariable[] variables = StandardTaskVariable.values();
        Map<TaskVariable, StandardTaskVariable> result
                = new HashMap<TaskVariable, StandardTaskVariable>(variables.length * 2);
        for (StandardTaskVariable variable: variables) {
            result.put(variable.getVariable(), variable);
        }
        return result;
    }

    public static TaskVariableMap createVarReplaceMap(
            final NbGradleProject project, final Lookup actionContext) {
        if (project == null) throw new NullPointerException("project");
        if (actionContext == null) throw new NullPointerException("actionContext");

        final ConcurrentMap<TaskVariable, VariableValue> cache
                = new ConcurrentHashMap<TaskVariable, VariableValue>();

        return new TaskVariableMap() {
            @Override
            public String tryGetValueForVariable(TaskVariable variable) {
                StandardTaskVariable stdVar = TASK_VARIABLE_MAP.get(variable);
                if (stdVar == null) {
                    return null;
                }

                VariableValue result = cache.get(variable);
                if (result == null) {
                    result = stdVar.tryGetValue(this, project, actionContext);

                    VariableValue prevResult = cache.putIfAbsent(variable, result);
                    if (prevResult != null) {
                        result = prevResult;
                    }
                }
                return result.value;
            }
        };
    }

    public static String replaceVars(
            String str, TaskVariableMap varReplaceMap) {
        StringBuilder result = new StringBuilder(str.length() * 2);

        int index = 0;
        while (index < str.length()) {
            char ch = str.charAt(index);
            if (ch == '$') {
                int varStart = str.indexOf('{', index + 1);
                int varEnd = varStart >= 0 ? str.indexOf('}', varStart + 1) : -1;
                if (varStart >= 0 && varEnd >= varStart) {
                    String varName = str.substring(varStart + 1, varEnd);
                    if (TaskVariable.isValidVariableName(varName)) {
                        String value = varReplaceMap.tryGetValueForVariable(new TaskVariable(varName));
                        if (value != null) {
                            result.append(value);
                            index = varEnd + 1;
                            continue;
                        }
                    }
                }
            }

            result.append(ch);
            index++;
        }
        return result.toString();
    }

    private final TaskVariable variable;
    private final ValueGetter valueGetter;

    private StandardTaskVariable(String variableName, ValueGetter valueGetter) {
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

    private VariableValue tryGetValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
        return valueGetter.tryGetValue(variables, project, actionContext);
    }

    private static abstract class ValueGetter {
        public abstract VariableValue tryGetValue(
                TaskVariableMap variables,
                NbGradleProject project,
                Lookup actionContext);
    }

    private static final class VariableValue {
        public final String value;

        public VariableValue(String value) {
            this.value = value;
        }
    }
}
