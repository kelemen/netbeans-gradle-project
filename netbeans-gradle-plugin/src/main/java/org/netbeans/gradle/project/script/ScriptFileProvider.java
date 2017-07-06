package org.netbeans.gradle.project.script;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ScriptFileProvider {
    public boolean isScriptFileName(String fileName);

    public Path findScriptFile(Path baseDir, String baseName);
    public Iterable<Path> findScriptFiles(Path baseDir, String baseName);

    public Collection<Path> findScriptFiles(
            Path baseDir,
            Predicate<? super String> baseNameFilter) throws IOException;

    public void findScriptFiles(
            Path baseDir,
            Predicate<? super String> baseNameFilter,
            Consumer<Path> fileProcessor) throws IOException;
}
