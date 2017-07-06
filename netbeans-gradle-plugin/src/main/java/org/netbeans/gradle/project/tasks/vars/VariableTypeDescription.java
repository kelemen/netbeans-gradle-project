package org.netbeans.gradle.project.tasks.vars;

import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.util.StringUtils;

public final class VariableTypeDescription {
    public static final String TYPE_NAME_STRING = "string";
    public static final String TYPE_NAME_ENUM = "enum";
    public static final String TYPE_NAME_BOOL = "bool";

    public static final VariableTypeDescription DEFAULT_TYPE = new VariableTypeDescription(TYPE_NAME_STRING, "");

    private final String typeName;
    private final String escapedTypeArguments;

    public VariableTypeDescription(String typeName, String escapedTypeArguments) {
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(escapedTypeArguments, "escapedTypeArguments");

        this.typeName = typeName.toLowerCase(Locale.ROOT);
        this.escapedTypeArguments = escapedTypeArguments;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getEscapedTypeArguments() {
        return escapedTypeArguments;
    }

    public boolean isDefault() {
        return equals(VariableTypeDescription.DEFAULT_TYPE);
    }

    private static String escapeCharacter(String str, char toEscape) {
        StringBuilder result = new StringBuilder(str.length());

        int pos = 0;
        while (pos < str.length()) {
            int charPos = StringUtils.unescapedIndexOf(str, pos, toEscape);
            if (charPos < 0) {
                result.append(str.substring(pos, str.length()));
                break;
            }

            result.append(str.substring(pos, charPos));
            result.append('\\');
            result.append(toEscape);
            pos = charPos + 1;
        }

        return result.toString();
    }

    @Nonnull
    public String getScriptString() {
        if (escapedTypeArguments.isEmpty()) {
            return typeName;
        }

        String formattedTypeArguments = escapedTypeArguments;
        formattedTypeArguments = escapeCharacter(formattedTypeArguments, ']');
        formattedTypeArguments = escapeCharacter(formattedTypeArguments, '}');

        return typeName + ":" + formattedTypeArguments;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + typeName.hashCode();
        hash = 37 * hash + escapedTypeArguments.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final VariableTypeDescription other = (VariableTypeDescription)obj;

        return this.typeName.equals(other.typeName)
                && this.escapedTypeArguments.equals(other.escapedTypeArguments);
    }
}
