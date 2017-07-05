package org.netbeans.gradle.project.util;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class SwingTestsRule implements TestRule {
    private static final SwingTestsRule INSTANCE = new SwingTestsRule();

    private SwingTestsRule() {
    }

    public static SwingTestsRule create() {
        return INSTANCE;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        if (description.getAnnotation(SwingTest.class) == null) {
            return base;
        }

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (SwingUtilities.isEventDispatchThread()) {
                    base.evaluate();
                    return;
                }

                try {
                    Thread.interrupted();
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            base.evaluate();
                        } catch (Throwable ex) {
                            throw new TestExceptionWrapper(ex);
                        }
                    });
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof TestExceptionWrapper) {
                        throw cause.getCause();
                    }
                    else {
                        throw cause;
                    }
                }
            }
        };
    }

    private static class TestExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
