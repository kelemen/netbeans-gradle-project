package org.netbeans.gradle.project.util;

public interface NbFunction<ArgType, ResultType> {
    public ResultType call(ArgType arg);
}
