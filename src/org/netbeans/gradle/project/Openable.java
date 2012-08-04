package org.netbeans.gradle.project;

public interface Openable extends java.io.Closeable {
    public void open();
    @Override
    public void close();
}
