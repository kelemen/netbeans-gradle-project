
package org.netbeans.gradle.model.internal;

import org.gradle.tooling.BuildController;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.BuilderUtils;

/**
 * These builders are for testing purposes only. That is, they must be in the
 * source packages (instead of test packages) so tests can pass them objects
 * which can only be deserialized when having the test classes on the classpath
 * while the builders themselves are always deserializable.
 */
public final class ConstBuilders {
    public static ProjectInfoBuilder2<Object> constProjectInfoBuilder(Object result) {
        return new ConstProjectInfoBuilder(result);
    }

    public static BuildInfoBuilder<Object> constBuildInfoBuilder(Object result) {
        return new ConstBuildInfoBuilder(result);
    }

    private static final class ConstProjectInfoBuilder implements ProjectInfoBuilder2<Object> {
        private static final long serialVersionUID = 1L;

        private final Object result;

        public ConstProjectInfoBuilder(Object result) {
            if (result == null) throw new NullPointerException("result");
            this.result = result;
        }

        @Override
        public Object getProjectInfo(Object project) {
            return result;
        }

        @Override
        public String getName() {
            return BuilderUtils.getNameForGenericBuilder(this, result.toString());
        }
    }

    private static final class ConstBuildInfoBuilder implements BuildInfoBuilder<Object> {
        private static final long serialVersionUID = 1L;

        private final Object result;

        public ConstBuildInfoBuilder(Object result) {
            if (result == null) throw new NullPointerException("result");
            this.result = result;
        }

        @Override
        public Object getInfo(BuildController controller) {
            return result;
        }

        @Override
        public String getName() {
            return BuilderUtils.getNameForGenericBuilder(this, result.toString());
        }
    }

    private ConstBuilders() {
        throw new AssertionError();
    }
}
