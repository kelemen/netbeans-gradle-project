package org.netbeans.gradle.project.tasks.vars;

public interface StringResolver {
    public String resolveString(String str);
    public String resolveStringIfValid(String str);
}
