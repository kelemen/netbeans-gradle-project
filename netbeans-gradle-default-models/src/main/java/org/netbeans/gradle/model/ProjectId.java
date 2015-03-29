package org.netbeans.gradle.model;

import java.io.Serializable;

public final class ProjectId implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String group;
    private final String name;
    private final String version;

    public ProjectId(String group, String name, String version) {
        if (group == null) throw new NullPointerException("group");
        if (name == null) throw new NullPointerException("name");
        if (version == null) throw new NullPointerException("version");

        this.group = group;
        this.name = name;
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
