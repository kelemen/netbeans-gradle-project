package org.netbeans.gradle.model;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import org.gradle.tooling.LongRunningOperation;
import org.gradle.tooling.ProgressListener;

public final class BuildOperationArgs {
    private OutputStream standardOutput;
    private OutputStream standardError;
    private InputStream standardInput;
    private File javaHome;
    private String[] jvmArguments;
    private String[] arguments;
    private ProgressListener[] progressListeners = new ProgressListener[0];

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

    public void setJvmArguments(String... jvmArguments) {
        this.jvmArguments = jvmArguments != null ? jvmArguments.clone() : null;
    }

    public String[] getArguments() {
        return arguments != null ? arguments.clone() : null;
    }

    public void setArguments(String... arguments) {
        this.arguments = arguments != null ? arguments.clone() : null;
    }

    public ProgressListener[] getProgressListeners() {
        return progressListeners.clone();
    }

    public void setProgressListeners(ProgressListener[] progressListeners) {
        this.progressListeners = progressListeners.clone();
    }

    public void setupLongRunningOP(LongRunningOperation op) {
        if (javaHome != null) {
            op.setJavaHome(javaHome);
        }

        if (arguments != null) {
            op.withArguments(arguments.clone());
        }

        if (jvmArguments != null) {
            op.setJvmArguments(jvmArguments.clone());
        }

        if (standardOutput != null) {
            op.setStandardOutput(standardOutput);
        }

        if (standardError != null) {
            op.setStandardError(standardError);
        }

        if (standardInput != null) {
            op.setStandardInput(standardInput);
        }

        for (ProgressListener listener: progressListeners) {
            op.addProgressListener(listener);
        }
    }
}
