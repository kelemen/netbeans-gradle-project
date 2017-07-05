package org.netbeans.gradle.project.tasks;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.view.GlobalErrorReporter;

public final class GradleDaemonFailures {
    private static final DaemonFailureHandler DEFAULT_HANDLER = new MultiHandler(new DaemonFailureHandler[] {
        failureDueToJarCachingHandler()
    });

    public static DaemonFailureHandler getDefaultHandler() {
        return DEFAULT_HANDLER;
    }

    private static DaemonFailureHandler failureDueToJarCachingHandler() {
        String pattern = "JAR entry .*BuildAction.*\\.class not found in .*\\.jar";
        return new RootCauseMessagePatternHandler(pattern, (Throwable failure) -> {
            GlobalErrorReporter.showIssue(NbStrings.getCachedJarIssueMessage());
            return true;
        });
    }

    private static final class RootCauseMessagePatternHandler implements DaemonFailureHandler {
        private final Pattern pattern;
        private final DaemonFailureHandler handlerOnMatch;

        public RootCauseMessagePatternHandler(String pattern, DaemonFailureHandler handlerOnMatch) {
            this(Pattern.compile(pattern), handlerOnMatch);
        }

        public RootCauseMessagePatternHandler(Pattern pattern, DaemonFailureHandler handlerOnMatch) {
            assert pattern != null;
            assert handlerOnMatch != null;

            this.pattern = pattern;
            this.handlerOnMatch = handlerOnMatch;
        }

        @Override
        public boolean tryHandleFailure(Throwable failure) {
            Throwable rootCause = Exceptions.getRootCause(failure);
            if (rootCause instanceof FileNotFoundException) {
                String message = rootCause.getMessage();
                if (message != null) {
                    if (pattern.matcher(message).matches()) {
                        return handlerOnMatch.tryHandleFailure(failure);
                    }
                }
            }
            return false;
        }
    }

    private static final class MultiHandler implements DaemonFailureHandler {
        private final DaemonFailureHandler[] handlers;

        public MultiHandler(DaemonFailureHandler[] handlers) {
            this.handlers = handlers.clone();
            CollectionUtils.checkNoNullElements(Arrays.asList(this.handlers), "handlers");
        }

        @Override
        public boolean tryHandleFailure(Throwable failure) {
            for (DaemonFailureHandler handler: handlers) {
                if (handler.tryHandleFailure(failure)) {
                    return true;
                }
            }
            return false;
        }
    }

    private GradleDaemonFailures() {
        throw new AssertionError();
    }
}
