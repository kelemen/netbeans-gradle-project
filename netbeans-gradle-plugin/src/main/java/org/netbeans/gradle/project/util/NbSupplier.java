package org.netbeans.gradle.project.util;

public interface NbSupplier<ResultType> {
    public ResultType get();
}
