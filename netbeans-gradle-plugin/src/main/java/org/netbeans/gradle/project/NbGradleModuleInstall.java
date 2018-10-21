package org.netbeans.gradle.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class NbGradleModuleInstall extends Yenta {
    private static final long serialVersionUID = 1L;
    private static final Set<String> FRIENDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "org.netbeans.modules.gsf.testrunner",
            "org.netbeans.modules.gsf.testrunner.ui",
            "org.netbeans.modules.gsf.codecoverage",
            "org.netbeans.modules.groovy.support",
            "org.netbeans.modules.java.api.common",
            "org.netbeans.modules.java.preprocessorbridge")));

    @Override
    protected Set<String> friends() {
        return FRIENDS;
    }

}
