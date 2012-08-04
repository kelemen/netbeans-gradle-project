package org.netbeans.gradle.project.query;

import org.netbeans.gradle.project.Openable;

public interface AsyncQuery<InputType, ResultType> extends Openable {
    public AsyncNoArgQuery<ResultType> getQuery(InputType input);
}
