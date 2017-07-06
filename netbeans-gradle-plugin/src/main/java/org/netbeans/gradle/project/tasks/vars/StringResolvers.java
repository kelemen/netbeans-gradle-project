package org.netbeans.gradle.project.tasks.vars;

import java.util.Objects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.openide.util.Lookup;

public final class StringResolvers {
    private static final StringResolver GLOBAL_RESOLVER = bindVariableResolver(
            VariableResolvers.getDefault(),
            TaskVariableMaps.getGlobalVariableMap());

    private static final StringResolverSelector DEFAULT_RESOLVER_SELECTOR = new StringResolverSelector() {
        @Override
        public StringResolver getContextFreeResolver() {
            return GLOBAL_RESOLVER;
        }

        @Override
        public StringResolver getProjectResolver(NbGradleProject project, Lookup context) {
            TaskVariableMap varMap = TaskVariableMaps.createProjectActionVariableMap(project, context);
            return bindVariableResolver(VariableResolvers.getDefault(), varMap);
        }
    };

    public static StringResolverSelector getDefaultResolverSelector() {
        return DEFAULT_RESOLVER_SELECTOR;
    }

    public static StringResolver getDefaultGlobalResolver() {
        return getDefaultResolverSelector().getContextFreeResolver();
    }

    public static StringResolver bindVariableResolver(final VariableResolver resolver, final TaskVariableMap variables) {
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(variables, "variables");

        return new StringResolver() {
            @Override
            public String resolveString(String str) {
                return resolver.replaceVars(str, variables);
            }

            @Override
            public String resolveStringIfValid(String str) {
                return resolver.replaceVarsIfValid(str, variables);
            }
        };
    }

    private StringResolvers() {
        throw new AssertionError();
    }
}
