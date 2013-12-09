package org.netbeans.gradle.model.java;

import org.netbeans.gradle.model.util.Exceptions;

public final class JavaModelTests {
    private static void checkNoIssue(Throwable issue) {
        if (issue != null) {
            throw Exceptions.throwUnchecked(issue);
        }
    }

    public static void checkNoDependencyResolultionError(JavaSourcesModel sources) {
        for (JavaSourceSet sourceSet: sources.getSourceSets()) {
            checkNoIssue(sourceSet.getCompileClassPathProblem());
            checkNoIssue(sourceSet.getRuntimeClassPathProblem());
        }
    }

    private JavaModelTests() {
        throw new AssertionError();
    }
}
