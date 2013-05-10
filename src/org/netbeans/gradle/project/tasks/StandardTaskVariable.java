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
    PROJECT_NAME("project") {
        @Override
        public String tryGetValue(NbGradleProject project, Lookup actionContext) {
            String uniqueName = project.getAvailableModel().getMainModule().getUniqueName();
            if (":".equals(uniqueName)) { // This is the root project.
                uniqueName = "";
            }
            return uniqueName;
        }
    },
    SELECTED_CLASS("selected-class") {
        @Override
        public String tryGetValue(NbGradleProject project, Lookup actionContext) {
            FileObject file = getFileOfContext(actionContext);
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

            return relFileName != null ? relFileName.replace('/', '.') : null;
        }
    },
    TEST_FILE_PATH("test-file-path") {
        @Override
        public String tryGetValue(NbGradleProject project, Lookup actionContext) {
            String selectedClass = SELECTED_CLASS.tryGetValue(project, actionContext);
            return deduceFromClass(selectedClass);
        }

        private String deduceFromClass(String selectedClass) {
            return selectedClass != null
                    ? selectedClass.replace('.', '/')
                    : null;
        }

        @Override
        protected String tryDeduceFrom(TaskVariableMap variables, ConcurrentMap<TaskVariable, VariableValue> cache) {
            String selectedClass = variables.tryGetValueForVariable(SELECTED_CLASS.getVariable());
            cache.putIfAbsent(SELECTED_CLASS.getVariable(), new VariableValue(selectedClass));
            return deduceFromClass(selectedClass);
        }
    },
    PLATFORM_DIR("platform-dir") {
        @Override
        public String tryGetValue(NbGradleProject project, Lookup actionContext) {
            FileObject rootFolder = project.getProperties().getPlatform().getValue().getRootFolder();
            return rootFolder != null ? FileUtil.getFileDisplayName(rootFolder) : null;
        }
    };

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
                    String value = stdVar.tryDeduceFrom(this, cache);
                    if (value == null) {
                        value = stdVar.tryGetValue(project, actionContext);
                    }

                    cache.putIfAbsent(variable, new VariableValue(value));
                    result = cache.get(variable);
                }
                return result.getValue();
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

    private StandardTaskVariable(String variableName) {
        this.variable = new TaskVariable(variableName);
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

    public abstract String tryGetValue(NbGradleProject project, Lookup actionContext);

    protected String tryDeduceFrom(TaskVariableMap variables, ConcurrentMap<TaskVariable, VariableValue> cache) {
        return null;
    }

    private static final class VariableValue {
        private final String value;

        public VariableValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
