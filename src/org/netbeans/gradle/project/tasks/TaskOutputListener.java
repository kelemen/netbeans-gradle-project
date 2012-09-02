package org.netbeans.gradle.project.tasks;

public interface TaskOutputListener {
    public void receiveOutput(char[] buffer, int offset, int length);
}
