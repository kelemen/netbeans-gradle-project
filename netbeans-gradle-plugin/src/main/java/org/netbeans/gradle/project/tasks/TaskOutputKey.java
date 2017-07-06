package org.netbeans.gradle.project.tasks;

import java.util.Arrays;
import java.util.Objects;
import org.netbeans.gradle.project.api.task.TaskKind;

public final class TaskOutputKey {
    private static final Object[] NO_OBJECTS = new Object[0];

    private final TaskKind kind;
    private final Object[] otherKeys;

    public TaskOutputKey(TaskKind kind, Object... otherKeys) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.otherKeys = cloneToObjectArray(otherKeys);
    }

    private Object[] cloneToObjectArray(Object[] objects) {
        if (objects.length == 0) {
            return NO_OBJECTS;
        }

        Object[] result = new Object[objects.length];
        System.arraycopy(objects, 0, result, 0, result.length);
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + kind.hashCode();
        hash = 67 * hash + Arrays.hashCode(otherKeys);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (getClass() != obj.getClass()) return false;

        final TaskOutputKey other = (TaskOutputKey)obj;
        if (this.kind != other.kind) return false;

        return this.otherKeys == other.otherKeys
                || Arrays.equals(this.otherKeys, other.otherKeys);
    }
}
