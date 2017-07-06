package org.netbeans.gradle.project.properties.standard;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jtrim2.utils.ExceptionHelper;

public final class MemCustomVariables implements CustomVariables {
    public static final CustomVariables EMPTY = new MemCustomVariables(Collections.<CustomVariable>emptySet());

    private final Map<String, CustomVariable> variables;

    public MemCustomVariables(Collection<CustomVariable> variables) {
        this.variables = toMap(variables);
        ExceptionHelper.checkNotNullElements(this.variables.values(), "variables");
    }

    private static Map<String, CustomVariable> toMap(Collection<CustomVariable> variables) {
        int count = variables.size();
        if (count == 0) {
            return Collections.emptyMap();
        }

        final double loadFactor = 0.75;
        int capacity = (int)((double)count / loadFactor) + 1;
        Map<String, CustomVariable> result = new LinkedHashMap<>(capacity);

        for (CustomVariable var: variables) {
            result.put(var.getName(), var);
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean isEmpty() {
        return variables.isEmpty();
    }

    @Override
    public String tryGetValue(String name) {
        CustomVariable result = variables.get(name);
        return result != null ? result.getValue() : null;
    }

    @Override
    public Collection<CustomVariable> getVariables() {
        return variables.values();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.variables);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final MemCustomVariables other = (MemCustomVariables)obj;
        return Objects.equals(this.variables, other.variables);
    }

    @Override
    public String toString() {
        return getVariables().toString();
    }
}
