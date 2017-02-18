package org.netbeans.gradle.project.tasks.vars;

public final class VariableResolvers {
    private static final VariableResolver LENIENT_RESOLVER = new LenientVariableResolver();

    public static VariableResolver getDefault() {
        return getLenientVariableResolver();
    }

    public static VariableResolver getLenientVariableResolver() {
        return LENIENT_RESOLVER;
    }

    private VariableResolvers() {
        throw new AssertionError();
    }
}
