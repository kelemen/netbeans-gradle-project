package org.netbeans.gradle.project;

import java.util.Collections;
import java.util.Set;

public final class NbGradleModuleInstall extends Yenta {
    private static final long serialVersionUID = 1L;

    @Override
    protected Set<String> friends() {
        return Collections.singleton("org.netbeans.modules.gsf.testrunner");
    }
}
