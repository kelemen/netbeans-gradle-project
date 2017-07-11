package org.netbeans.gradle.build;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepository;
import org.gradle.api.Project;

public final class GitWrapper implements Closeable {
    private final FileRepository repo;
    private final Git git;

    public GitWrapper(Project project) throws IOException {
        this(new File(project.getRootDir(), ".git"));
    }

    public GitWrapper(File gitDir) throws IOException {
        this.repo = new FileRepository(gitDir);
        this.git = new Git(this.repo);
    }

    public Git git() {
        return git;
    }

    public String tryGetCommitFor(String path) throws IOException {
        ObjectId id = git.getRepository().resolve(path);
        return id != null ? id.getName() : null;
    }

    @Override
    public void close() throws IOException {
        repo.close();
    }
}
