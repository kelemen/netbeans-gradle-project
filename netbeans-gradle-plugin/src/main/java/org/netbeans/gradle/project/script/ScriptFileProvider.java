package org.netbeans.gradle.project.script;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbPredicate;

public interface ScriptFileProvider {
    public boolean isScriptFileName(String fileName);

    public Path findScriptFile(Path baseDir, String baseName);
    public Iterable<Path> findScriptFiles(Path baseDir, String baseName);

    public Collection<Path> findScriptFiles(
            Path baseDir,
            NbPredicate<? super String> baseNameFilter) throws IOException;

    public void findScriptFiles(
            Path baseDir,
            NbPredicate<? super String> baseNameFilter,
            NbConsumer<Path> fileProcessor) throws IOException;
}
