package org.netbeans.gradle.project.others.test;

import java.util.Set;
import javax.swing.event.ChangeListener;

/**
 * Has the same methods as org.netbeans.modules.gsf.testrunner.api.RerunHandler.
 */
public interface RerunHandler {
    public void rerun();
    public void rerun(Set<?> tests);
    // Argument type is org.netbeans.modules.gsf.testrunner.api.RerunType.
    public boolean enabled(Object type);
    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);
}
