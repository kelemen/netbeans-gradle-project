package org.netbeans.gradle.project.tasks;

import java.util.Locale;
import javax.annotation.Nonnull;

public final class VariableTypeDescription {
    public static final String TYPE_NAME_STRING = "string";
    public static final String TYPE_NAME_ENUM = "enum";
    public static final String TYPE_NAME_BOOL = "bool";

    public static final VariableTypeDescription DEFAULT_TYPE = new VariableTypeDescription(TYPE_NAME_STRING, "");

    private final String typeName;
    private final String typeArguments;

    public VariableTypeDescription(String typeName, String typeArguments) {
        if (typeName == null) throw new NullPointerException("typeName");
        if (typeArguments == null) throw new NullPointerException("typeArguments");

        this.typeName = typeName.toLowerCase(Locale.ROOT);
        this.typeArguments = typeArguments;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeArguments() {
        return typeArguments;
    }

    public boolean isDefault() {
        return equals(VariableTypeDescription.DEFAULT_TYPE);
    }

    @Nonnull
    public String getScriptString() {
        if (typeArguments.isEmpty()) {
            return typeName;
        }

        String escapedTypeArguments = typeArguments
                .replace("\\", "\\\\")
                .replace("]", "\\]")
                .replace("}", "\\}");
        return typeName + ":" + escapedTypeArguments;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + typeName.hashCode();
        hash = 37 * hash + typeArguments.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final VariableTypeDescription other = (VariableTypeDescription)obj;

        return this.typeName.equals(other.typeName)
                && this.typeArguments.equals(other.typeArguments);
    }
}
