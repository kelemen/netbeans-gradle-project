package org.netbeans.gradle.project.query;

public interface AsyncQuery<InputType, ResultType> {
    public AsyncNoArgQuery<ResultType> getQuery(InputType input);

    public void open();
    public void close();
}
