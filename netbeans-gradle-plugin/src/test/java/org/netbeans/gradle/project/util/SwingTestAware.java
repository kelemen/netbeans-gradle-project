package org.netbeans.gradle.project.util;

import org.junit.Rule;
import org.junit.rules.TestRule;

public abstract class SwingTestAware {
    @Rule
    public final TestRule swingRule = SwingTestsRule.create();
}
