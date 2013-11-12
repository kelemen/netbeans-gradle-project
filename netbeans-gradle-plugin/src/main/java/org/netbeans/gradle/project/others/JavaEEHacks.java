package org.netbeans.gradle.project.others;

import java.util.Collection;
import java.util.Collections;
import org.netbeans.api.project.Project;

public final class JavaEEHacks {
    private static final ProjectLookupObject CDI_UTIL = new ProjectLookupObject(new PluginClass(
            OtherPlugins.WEB_BEANS,
            "org.netbeans.modules.web.beans.CdiUtil"));

    public static Collection<Object> getAdditionalProjectLookup(Project project) {
        Object cdiUtil = CDI_UTIL.tryCreateInstance(project);
        return cdiUtil != null
                ? Collections.singleton(cdiUtil)
                : Collections.emptySet();
    }

    private JavaEEHacks() {
        throw new AssertionError();
    }
}
