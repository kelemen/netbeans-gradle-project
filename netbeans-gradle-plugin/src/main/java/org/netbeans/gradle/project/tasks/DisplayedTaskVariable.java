package org.netbeans.gradle.project.tasks;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.StringUtils;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

public final class DisplayedTaskVariable {
    private static final char START_TYPE_CHAR = '[';
    private static final char END_TYPE_CHAR = ']';
    private static final char DISPLAY_NAME_SEPARATOR = ':';

    private final TaskVariable variable;
    private final String displayName;
    private final VariableTypeDescription typeDescription;

    public DisplayedTaskVariable(
            TaskVariable variable,
            String displayName,
            VariableTypeDescription typeDescription) {

        if (variable == null) throw new NullPointerException("variable");
        if (displayName == null) throw new NullPointerException("displayName");
        if (typeDescription == null) throw new NullPointerException("typeDescription");

        this.variable = variable;
        this.displayName = displayName;
        this.typeDescription = typeDescription;
    }

    public TaskVariable getVariable() {
        return variable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public VariableTypeDescription getTypeDescription() {
        return typeDescription;
    }

    public boolean isDefault() {
        return variable.getVariableName().equals(displayName)
                && typeDescription.isDefault();
    }

    @Nonnull
    public String getScriptReplaceConstant() {
        String varName = variable.getVariableName();

        StringBuilder result = new StringBuilder();
        result.append("${");
        result.append(varName);
        if (!typeDescription.isDefault()) {
            result.append(START_TYPE_CHAR);
            result.append(typeDescription.getScriptString());
            result.append(END_TYPE_CHAR);
        }

        if (!varName.equals(displayName)) {
            result.append(DISPLAY_NAME_SEPARATOR);

            String escapedDisplayName = displayName
                .replace("\\", "\\\\")
                .replace("}", "\\}");
            result.append(escapedDisplayName);
        }

        result.append("}");

        return result.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + variable.hashCode();
        hash = 97 * hash + displayName.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final DisplayedTaskVariable other = (DisplayedTaskVariable)obj;

        return this.variable.equals(other.variable)
                && this.displayName.equals(other.displayName);
    }

    @Override
    public String toString() {
        return getScriptReplaceConstant();
    }

    public static TaskVariableMap variableMap(Map<DisplayedTaskVariable, String> map) {
        final Map<TaskVariable, String> appliedMap = new HashMap<>();
        for (Map.Entry<DisplayedTaskVariable, String> entry: map.entrySet()) {
            appliedMap.put(entry.getKey().getVariable(), entry.getValue());
        }

        return new TaskVariableMap() {
            @Override
            public String tryGetValueForVariable(TaskVariable variable) {
                return appliedMap.get(variable);
            }
        };
    }

    private static int unescapedIndexOf(String str, int startIndex, char toFind) {
        return StringUtils.unescapedIndexOf(str, startIndex, toFind);
    }

    private static String normalizeEscapedString(String str) {
        String result = StringUtils.unescapeString(str);
        return result.trim();
    }

    private static VariableTypeDescription parseType(String typeDef) {
        String typeName;
        String typeArguments;

        int descrSeparatorIndex = unescapedIndexOf(typeDef, 0, ':');
        if (descrSeparatorIndex >= 0) {
            typeName = typeDef.substring(0, descrSeparatorIndex);
            typeArguments = typeDef.substring(descrSeparatorIndex + 1, typeDef.length());
        }
        else {
            typeName = typeDef;
            typeArguments = "";
        }

        typeName = typeName.trim();
        typeArguments = typeArguments.trim();

        return new VariableTypeDescription(typeName, typeArguments);
    }

    public static DisplayedTaskVariable tryParseTaskVariable(String varDef) {
        // It is expected by later code that the string is not empty.
        if (varDef.isEmpty()) {
            return null;
        }

        // variableName[type: typeDescr]: displayName

        String varName;
        String displayName;
        VariableTypeDescription typeDescr;

        int typeStartIndex = unescapedIndexOf(varDef, 0, START_TYPE_CHAR);
        int nameSeparatorIndex = unescapedIndexOf(varDef, 0, DISPLAY_NAME_SEPARATOR);

        if (nameSeparatorIndex >= 0 && typeStartIndex >= 0) {
            if (nameSeparatorIndex < typeStartIndex) {
                // variableName: Display Name[2]
                varName = varDef.substring(0, nameSeparatorIndex);
                displayName = varDef.substring(nameSeparatorIndex + 1, varDef.length());
                typeDescr = VariableTypeDescription.DEFAULT_TYPE;
            }
            else {
                int typeEndIndex = unescapedIndexOf(varDef, typeStartIndex, END_TYPE_CHAR);

                varName = varDef.substring(0, typeStartIndex);

                if (typeEndIndex > typeStartIndex) {
                    nameSeparatorIndex = unescapedIndexOf(varDef, typeEndIndex, DISPLAY_NAME_SEPARATOR);
                    if (nameSeparatorIndex < 0) {
                        // Missing ':' to separate display name
                        // Could be because there is no display name.

                        // E.g.: variableName[typeDescr: abcd] Display Name
                        //       variableName[typeDescr: abcd]
                        nameSeparatorIndex = typeEndIndex;
                    }
                    // Else standard: variableName[typeDescr]: Display Name

                    displayName = varDef.substring(nameSeparatorIndex + 1, varDef.length());
                    typeDescr = parseType(varDef.substring(typeStartIndex + 1, typeEndIndex));
                }
                else {
                    // E.g.: variableName[unclosed typedef: abcd
                    // Assume a ']' character after the end.
                    typeDescr = parseType(varDef.substring(typeStartIndex + 1, varDef.length()));
                    displayName = "";
                }
            }
        }
        else if (typeStartIndex >= 0) {
            varName = varDef.substring(0, typeStartIndex);
            displayName = "";

            int typeEndIndex = unescapedIndexOf(varDef, typeStartIndex, ']');
            if (typeEndIndex < 0) {
                // E.g.: variableName[unclosed typedef
                // Assume a ']' character after the end.
                typeEndIndex = varDef.length();
            }
            // Else: variableName[typeDef]

            typeDescr = parseType(varDef.substring(typeStartIndex + 1, typeEndIndex));
        }
        else if (nameSeparatorIndex >= 0) {
            // variableName: Display Name
            varName = varDef.substring(0, nameSeparatorIndex);
            displayName = varDef.substring(nameSeparatorIndex + 1, varDef.length());
            typeDescr = VariableTypeDescription.DEFAULT_TYPE;
        }
        else {
            varName = varDef;
            displayName = "";
            typeDescr = VariableTypeDescription.DEFAULT_TYPE;
        }

        varName = varName.trim();
        displayName = normalizeEscapedString(displayName);
        if (displayName.isEmpty()) {
            displayName = varName;
        }

        if (!TaskVariable.isValidVariableName(varName)) {
            return null;
        }

        return new DisplayedTaskVariable(new TaskVariable(varName), displayName, typeDescr);
    }
}
