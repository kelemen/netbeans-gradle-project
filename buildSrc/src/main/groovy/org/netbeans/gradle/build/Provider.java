package org.netbeans.gradle.build;

public interface Provider<T> {
    public T get();
}
