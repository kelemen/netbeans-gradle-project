package org.netbeans.gradle.project.util;

public interface NbBiFunction<T, U, R> {
    public R apply(T t, U u);
}
