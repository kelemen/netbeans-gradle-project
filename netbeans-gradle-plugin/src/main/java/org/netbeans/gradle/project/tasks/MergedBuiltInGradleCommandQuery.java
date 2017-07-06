package org.netbeans.gradle.project.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;

public final class MergedBuiltInGradleCommandQuery implements BuiltInGradleCommandQuery {
    private final BuiltInGradleCommandQuery defaultBuiltInTasks;
    private final Supplier<? extends Collection<? extends BuiltInGradleCommandQuery>> extraQueries;

    public MergedBuiltInGradleCommandQuery(Supplier<? extends Collection<? extends BuiltInGradleCommandQuery>> extraQueries) {
        this.extraQueries = Objects.requireNonNull(extraQueries, "extraQueries");
        this.defaultBuiltInTasks = new DefaultBuiltInTasks();
    }

    private List<BuiltInGradleCommandQuery> getAllQueries() {
        List<BuiltInGradleCommandQuery> result = new ArrayList<>();
        result.addAll(extraQueries.get());
        result.add(defaultBuiltInTasks);
        return result;
    }

    @Override
    public Set<String> getSupportedCommands() {
        Set<String> result = new HashSet<>(32);
        for (BuiltInGradleCommandQuery query: getAllQueries()) {
            result.addAll(query.getSupportedCommands());
        }
        return result;

    }

    @Override
    public String tryGetDisplayNameOfCommand(String command) {
        for (BuiltInGradleCommandQuery query: getAllQueries()) {
            String displayName = query.tryGetDisplayNameOfCommand(command);
            if (displayName != null) {
                return displayName;
            }
        }
        return null;
    }

    @Override
    public GradleCommandTemplate tryGetDefaultGradleCommand(ProfileDef profileDef, String command) {
        for (BuiltInGradleCommandQuery query: getAllQueries()) {
            if (query.getSupportedCommands().contains(command)) {
                return query.tryGetDefaultGradleCommand(profileDef, command);
            }
        }
        return null;
    }

    @Override
    public CustomCommandActions tryGetCommandDefs(ProfileDef profileDef, String command) {
        for (BuiltInGradleCommandQuery query: getAllQueries()) {
            if (query.getSupportedCommands().contains(command)) {
                return query.tryGetCommandDefs(profileDef, command);
            }
        }
        return null;
    }
}
