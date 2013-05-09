package org.netbeans.gradle.project.tasks;

import java.util.regex.Pattern;

public final class TaskVariable {
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9\\-_\\.]+");

    private final String variableName;

    public TaskVariable(String variableName) {
        if (variableName == null) throw new NullPointerException("variableName");
        if (!VARIABLE_NAME_PATTERN.matcher(variableName).matches()) {
            throw new IllegalArgumentException();
        }

        this.variableName = variableName;
    }

    public static boolean isValidVariableName(String name) {
        return VARIABLE_NAME_PATTERN.matcher(name).matches();
    }

    public String getVariableName() {
        return variableName;
    }

    public String getScriptReplaceConstant() {
        return "${" + variableName + "}";
    }

    @Override
    public int hashCode() {
        return 85 + variableName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final TaskVariable other = (TaskVariable)obj;
        return this.variableName.equals(other.variableName);
    }
}
