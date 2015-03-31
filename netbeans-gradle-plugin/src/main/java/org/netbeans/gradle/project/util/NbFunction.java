package org.netbeans.gradle.project.util;

public interface NbFunction<ArgType, ResultType> {
    public ResultType apply(ArgType arg);
}
