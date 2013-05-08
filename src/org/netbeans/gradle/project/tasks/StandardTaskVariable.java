package org.netbeans.gradle.project.tasks;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.project.NbGradleProject;
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
            String value = SELECTED_CLASS.tryGetValue(project, actionContext);
            return value != null
                    ? deduceFrom(Collections.singletonMap(SELECTED_CLASS, value))
                    : null;
        }

        @Override
        public boolean canDeduceFrom(Map<StandardTaskVariable, String> variables) {
            return variables.containsKey(SELECTED_CLASS);
        }

        @Override
        public String deduceFrom(Map<StandardTaskVariable, String> variables) {
            String selectedClass = variables.get(SELECTED_CLASS);
            if (selectedClass != null) {
                return selectedClass.replace('.', '/');
            }
            else {
                return super.deduceFrom(variables);
            }
        }
    };

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

    public static Map<TaskVariable, String> createVarReplaceMap(
            NbGradleProject project, Lookup actionContext) {

        Map<StandardTaskVariable, String> currentValues
                = new EnumMap<StandardTaskVariable, String>(StandardTaskVariable.class);
        for (StandardTaskVariable variable: StandardTaskVariable.values()) {
            String value;
            if (variable.canDeduceFrom(currentValues)) {
                value = variable.deduceFrom(currentValues);
            }
            else {
                value = variable.tryGetValue(project, actionContext);
            }

            if (value != null) {
                currentValues.put(variable, value);
            }
        }

        Map<TaskVariable, String> result = new HashMap<TaskVariable, String>(2 * currentValues.size());
        for (Map.Entry<StandardTaskVariable, String> entry: currentValues.entrySet()) {
            result.put(entry.getKey().getVariable(), entry.getValue());
        }
        return result;
    }

    public static String replaceVars(
            String str, Map<TaskVariable, String> varReplaceMap) {
        StringBuilder result = new StringBuilder(str.length() * 2);

        int index = 0;
        while (index < str.length()) {
            char ch = str.charAt(index);
            if (ch == '$') {
                int varStart = str.indexOf('{', index + 1);
                int varEnd = varStart >= 0 ? str.indexOf('}', varStart + 1) : -1;
                if (varStart >= 0 && varEnd >= varStart) {
                    String varName = str.substring(varStart + 1, varEnd);
                    String value = varReplaceMap.get(new TaskVariable(varName));
                    if (value != null) {
                        result.append(value);
                        index = varEnd + 1;
                        continue;
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

    public boolean canDeduceFrom(Map<StandardTaskVariable, String> variables) {
        return false;
    }

    public String deduceFrom(Map<StandardTaskVariable, String> variables) {
        throw new IllegalArgumentException("Cannot deduce " + name() + " from " + variables);
    }
}
