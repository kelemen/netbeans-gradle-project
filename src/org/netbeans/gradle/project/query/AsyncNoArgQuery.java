package org.netbeans.gradle.project.query;

import javax.swing.event.ChangeListener;

public interface AsyncNoArgQuery<ResultType> {
    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);

    public ResultType query() throws QueryException;

    public void open();
    public void close();
}
