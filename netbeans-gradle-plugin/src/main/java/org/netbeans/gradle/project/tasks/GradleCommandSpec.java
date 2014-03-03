package org.netbeans.gradle.project.tasks;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class GradleCommandSpec {
    private final GradleTaskDef source;
    private final GradleTaskDef processed;

    public GradleCommandSpec(GradleTaskDef source, GradleTaskDef processed) {
        ExceptionHelper.checkNotNullArgument(source, "source");

        this.source = source;
        this.processed = processed;
    }

    @Nonnull
    public GradleTaskDef getSource() {
        return source;
    }

    @Nullable
    public GradleTaskDef getProcessed() {
        return processed;
    }

    public static GradleCommandSpecFactory adjustFactory(
            GradleCommandSpecFactory source,
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments) {
        return new CommandAdjusterFactory(source, taskNames, arguments, jvmArguments);
    }

    private static final class CommandAdjusterFactory implements GradleCommandSpecFactory {
        private final GradleCommandSpecFactory source;
        private final List<String> taskNames;
        private final List<String> arguments;
        private final List<String> jvmArguments;

        public CommandAdjusterFactory(
                GradleCommandSpecFactory source,
                List<String> taskNames,
                List<String> arguments,
                List<String> jvmArguments) {
            ExceptionHelper.checkNotNullArgument(source, "source");

            this.source = source;
            this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
            this.arguments = CollectionUtils.copyNullSafeList(arguments);
            this.jvmArguments = CollectionUtils.copyNullSafeList(jvmArguments);
        }


        @Override
        public String getDisplayName() {
            return source.getDisplayName();
        }

        @Override
        public GradleCommandSpec tryCreateCommandSpec(CancellationToken cancelToken) throws Exception {
            GradleCommandSpec original = source.tryCreateCommandSpec(cancelToken);
            if (original == null) {
                return null;
            }

            GradleTaskDef.Builder result = new GradleTaskDef.Builder(original.getSource());
            result.setTaskNames(taskNames);
            result.setArguments(arguments);
            result.setJvmArguments(jvmArguments);

            return new GradleCommandSpec(original.getSource(), result.create());
        }
    }
}
