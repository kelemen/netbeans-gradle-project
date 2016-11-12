package org.netbeans.gradle.project.util;

import org.netbeans.gradle.model.util.NbSupplier5;

public interface NbSupplier<ResultType> extends NbSupplier5<ResultType> {
    @Override
    public ResultType get();
}
