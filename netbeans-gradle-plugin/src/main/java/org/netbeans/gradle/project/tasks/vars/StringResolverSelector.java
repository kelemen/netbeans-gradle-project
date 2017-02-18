package org.netbeans.gradle.project.tasks.vars;

import org.netbeans.gradle.project.NbGradleProject;
import org.openide.util.Lookup;

public interface StringResolverSelector {
    public StringResolver getContextFreeResolver();
    public StringResolver getProjectResolver(NbGradleProject project, Lookup context);
}
