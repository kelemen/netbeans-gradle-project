package org.netbeans.gradle.project.util;

import java.io.File;
import java.io.IOException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class SafeTmpFolder implements TestRule {
    private final TemporaryFolder wrapped;

    public SafeTmpFolder() {
        this.wrapped = new TemporaryFolder();
    }

    public void create() throws IOException {
        wrapped.create();
    }

    private static File normalize(File file) throws IOException {
        return file.getCanonicalFile();
    }

    public File newFile(String fileName) throws IOException {
        return normalize(wrapped.newFile(fileName));
    }

    public File newFile() throws IOException {
        return normalize(wrapped.newFile());
    }

    public File newFolder(String folder) throws IOException {
        return normalize(wrapped.newFolder(folder));
    }

    public File newFolder(String... folderNames) throws IOException {
        return normalize(wrapped.newFolder(folderNames));
    }

    public File newFolder() throws IOException {
        return normalize(wrapped.newFolder());
    }

    public File getRoot() {
        try {
            return normalize(wrapped.getRoot());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void delete() {
        wrapped.delete();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return wrapped.apply(base, description);
    }
}
