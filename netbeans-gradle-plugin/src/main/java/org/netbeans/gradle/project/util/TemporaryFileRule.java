package org.netbeans.gradle.project.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class TemporaryFileRule implements TestRule {
    private final byte[] content;
    private final AtomicReference<Path> fileRef;

    public TemporaryFileRule(String content) {
        this(content, StandardCharsets.UTF_8);
    }

    public TemporaryFileRule(String content, Charset encoding) {
        this(content.getBytes(encoding));
    }

    public TemporaryFileRule(byte[] content) {
        this.content = content.clone();
        this.fileRef = new AtomicReference<>(null);
    }

    public Path getFile() {
        Path result = fileRef.get();
        if (result == null) {
            throw new IllegalStateException("Test is not running.");
        }
        return result;
    }

    private void runTest(Statement base) throws Throwable {
        Path file = null;
        try {
            file = Files.createTempFile("test-", ".txt");
            if (!fileRef.compareAndSet(null, file)) {
                throw new IllegalStateException("Concurrent evaluate call.");
            }
            Files.write(file, content);

            base.evaluate();
        } finally {
            if (file != null) {
                fileRef.set(null);
                Files.deleteIfExists(file);
            }
        }
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                runTest(base);
            }
        };
    }
}
