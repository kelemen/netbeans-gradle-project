package org.netbeans.gradle.project.query;

import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.Openable;

public interface AsyncNoArgQuery<ResultType> extends Openable {
    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);

    public ResultType query() throws QueryException;
}
