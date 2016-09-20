package org.netbeans.gradle.project.api.entry;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;

import static org.junit.Assert.*;

public class Latin2ProjectTest {
    private static final SampleProjectRule PROJECT_REF = SampleProjectRule.getStandardRule("latin2-project.zip");

    private static final String MODEL_FETCHER_PREFIX;

    static {
        // Contains all non-ascii Hungarian characters
        String floodTolerantMirrorDrill = "\u00E1rv\u00EDzt\u0171r\u0151t\u00FCk\u00F6rf\u00FAr\u00F3g\u00E9p";
        // Starting with lower case "u" will expand to the unicode escape on Windows.
        // So when this test is run on Windows, it will test this as well.
        MODEL_FETCHER_PREFIX = "u-nb-m-input-" + floodTolerantMirrorDrill;
    }

    @ClassRule
    public static final TestRule RULE = RuleChain.emptyRuleChain()
            .around(PROJECT_REF)
            .around(new ModelFetcherPrefixRule(MODEL_FETCHER_PREFIX));

    private NbGradleProject rootProject;

    @Before
    public void setUp() throws Exception {
        rootProject = PROJECT_REF.loadAndWaitSingleProject();
    }

    private static boolean isJavaExtensionActive(Project project) {
        GradleClassPathProvider javaCpProvider = project.getLookup().lookup(GradleClassPathProvider.class);
        return javaCpProvider != null;
    }

    @Test
    public void testHasLoadedJavaExtension() throws Exception {
        assertTrue("Java extension must be enabled for " + rootProject.getName(),
                isJavaExtensionActive(rootProject));
    }

    private static final class ModelFetcherPrefixRule implements TestRule {
        private final String prefix;

        public ModelFetcherPrefixRule(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        GenericModelFetcher.setModelInputPrefix(prefix);
                        base.evaluate();
                    } finally {
                        GenericModelFetcher.setDefaultPrefixes();
                    }
                }
            };
        }
    }
}