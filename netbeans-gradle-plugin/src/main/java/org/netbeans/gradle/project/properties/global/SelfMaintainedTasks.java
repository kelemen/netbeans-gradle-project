package org.netbeans.gradle.project.properties.global;

// This enum is awkwardly named to preserve backward compatibility with
// its predecessor property: omitInitScript (which could only have a true or false value)
public enum SelfMaintainedTasks {
    TRUE,
    FALSE,
    MANUAL
}
