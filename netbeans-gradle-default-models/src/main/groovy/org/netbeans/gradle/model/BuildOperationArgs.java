package org.netbeans.gradle.model;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import org.gradle.tooling.ProgressListener;

public final class BuildOperationArgs {
    private OutputStream standardOutput;
    private OutputStream standardError;
    private InputStream standardInput;
    private File javaHome;
    private String[] jvmArguments;
    private String[] arguments;
    private ProgressListener[] progressListeners;

    public OutputStream getStandardOutput() {
        return standardOutput;
    }

    public void setStandardOutput(OutputStream standardOutput) {
        this.standardOutput = standardOutput;
    }

    public OutputStream getStandardError() {
        return standardError;
    }

    public void setStandardError(OutputStream standardError) {
        this.standardError = standardError;
    }

    public InputStream getStandardInput() {
        return standardInput;
    }

    public void setStandardInput(InputStream standardInput) {
        this.standardInput = standardInput;
    }

    public File getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(File javaHome) {
        this.javaHome = javaHome;
    }

    public String[] getJvmArguments() {
        return jvmArguments != null ? jvmArguments.clone() : null;
    }

    public void setJvmArguments(String[] jvmArguments) {
        this.jvmArguments = jvmArguments;
    }

    public String[] getArguments() {
        return arguments != null ? arguments.clone() : null;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    public ProgressListener[] getProgressListeners() {
        return progressListeners != null ? progressListeners.clone() : null;
    }

    public void setProgressListeners(ProgressListener[] progressListeners) {
        this.progressListeners = progressListeners;
    }
}
