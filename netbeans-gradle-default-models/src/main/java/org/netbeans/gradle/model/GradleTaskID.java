package org.netbeans.gradle.model;

import java.io.Serializable;

public final class GradleTaskID implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String fullName;

    public GradleTaskID(String name, String fullName) {
        this.name = name;
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof GradleTaskID) {
            GradleTaskID otherTaskId = ((GradleTaskID) other);
            return String.valueOf(name).equals(String.valueOf(otherTaskId.name));
        }
        return false;
    }
}
