package org.netbeans.gradle.project.properties.standard;

import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class CustomVariable {
    private final String name;
    private final String value;

    public CustomVariable(String name, String value) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        ExceptionHelper.checkNotNullArgument(value, "value");

        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.name);
        hash = 23 * hash + Objects.hashCode(this.value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final CustomVariable other = (CustomVariable)obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return "CustomVariable{" + name + ": " + value + '}';
    }
}
