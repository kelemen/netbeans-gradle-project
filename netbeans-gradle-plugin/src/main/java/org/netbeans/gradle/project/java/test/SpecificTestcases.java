package org.netbeans.gradle.project.java.test;

import java.util.Collection;
import java.util.List;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class SpecificTestcases {
    private final List<SpecificTestcase> testcases;

    public SpecificTestcases(Collection<SpecificTestcase> testcases) {
        this.testcases = CollectionUtils.copyNullSafeList(testcases);
    }

    public List<SpecificTestcase> getTestcases() {
        return testcases;
    }
}
