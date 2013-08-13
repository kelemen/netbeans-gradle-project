package org.netbeans.gradle.project.tasks;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;

public final class MergedBuiltInGradleCommandQuery implements BuiltInGradleCommandQuery {
    private final NbGradleProject project;
    private final BuiltInGradleCommandQuery defaultBuiltInTasks;

    public MergedBuiltInGradleCommandQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.defaultBuiltInTasks = new DefaultBuiltInTasks(project);
    }

    private List<BuiltInGradleCommandQuery> getAllQueries() {
        List<BuiltInGradleCommandQuery> result = new LinkedList<BuiltInGradleCommandQuery>();
        result.addAll(project.getLookup().lookupAll(BuiltInGradleCommandQuery.class));
        result.add(defaultBuiltInTasks);
        return result;
    }

    @Override
    public Set<String> getSupportedCommands() {
        Set<String> result = new HashSet<String>(32);
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
            GradleCommandTemplate result = query.tryGetDefaultGradleCommand(profileDef, command);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public CustomCommandActions tryGetCommandDefs(ProfileDef profileDef, String command) {
        for (BuiltInGradleCommandQuery query: getAllQueries()) {
            CustomCommandActions result = query.tryGetCommandDefs(profileDef, command);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}
