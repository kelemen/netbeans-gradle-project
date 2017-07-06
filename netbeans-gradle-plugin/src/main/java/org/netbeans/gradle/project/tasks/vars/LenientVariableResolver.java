package org.netbeans.gradle.project.tasks.vars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.util.NbCollectionsEx;
import org.netbeans.gradle.project.util.StringUtils;

final class LenientVariableResolver implements VariableResolver {
    private static final char START_TYPE_CHAR = '[';
    private static final char END_TYPE_CHAR = ']';
    private static final char DISPLAY_NAME_SEPARATOR = ':';

    @Override
    public String replaceVars(String str, TaskVariableMap varReplaceMap) {
        return replaceVars(str, varReplaceMap, NbCollectionsEx.getDevNullCollection());
    }

    @Override
    public String replaceVars(String str, TaskVariableMap varReplaceMap, Collection<? super DisplayedTaskVariable> collectedVariables) {
        Objects.requireNonNull(str, "str");
        Objects.requireNonNull(varReplaceMap, "varReplaceMap");
        Objects.requireNonNull(collectedVariables, "collectedVariables");

        StringBuilder result = null;
        int index = 0;
        while (index < str.length()) {
            char ch = str.charAt(index);
            if (ch == '$') {
                int varStart = str.indexOf('{', index + 1);
                int varEnd = varStart >= 0
                        ? StringUtils.unescapedIndexOf(str, varStart + 1, '}')
                        : -1;
                if (varStart >= 0 && varEnd >= varStart) {
                    String varDef = str.substring(varStart + 1, varEnd);
                    DisplayedTaskVariable taskVar = tryParseTaskVariable(varDef);

                    if (taskVar != null) {
                        collectedVariables.add(taskVar);

                        int nextIndex = varEnd + 1;

                        String value = varReplaceMap.tryGetValueForVariable(taskVar.getVariable());
                        if (value != null) {
                            if (result == null) {
                                result = new StringBuilder(str.length() * 2);
                                result.append(str, 0, index);
                            }
                            result.append(value);
                        }
                        else {
                            if (result != null) {
                                result.append(str, index, nextIndex);
                            }
                        }
                        index = nextIndex;
                        continue;
                    }
                }
            }

            if (result != null) {
                result.append(ch);
            }

            index++;
        }
        return result != null ? result.toString() : str;
    }

    @Override
    public void collectVars(String str, TaskVariableMap varReplaceMap, Collection<? super DisplayedTaskVariable> collectedVariables) {
        replaceVars(str, varReplaceMap, collectedVariables);
    }

    @Override
    public String replaceVarsIfValid(String str, TaskVariableMap varReplaceMap) {
        List<DisplayedTaskVariable> vars = new ArrayList<>();
        String result = replaceVars(str, varReplaceMap, vars);
        for (DisplayedTaskVariable var: vars) {
            if (varReplaceMap.tryGetValueForVariable(var.getVariable()) == null) {
                return null;
            }
        }
        return result;
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

    @Nonnull
    public static String getScriptReplaceConstant(DisplayedTaskVariable displayedVar) {
        String varName = displayedVar.getVariable().getVariableName();

        StringBuilder result = new StringBuilder();
        result.append("${");
        result.append(varName);

        VariableTypeDescription typeDescription = displayedVar.getTypeDescription();
        if (!typeDescription.isDefault()) {
            result.append(START_TYPE_CHAR);
            result.append(typeDescription.getScriptString());
            result.append(END_TYPE_CHAR);
        }

        String displayName = displayedVar.getDisplayName();
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
}
