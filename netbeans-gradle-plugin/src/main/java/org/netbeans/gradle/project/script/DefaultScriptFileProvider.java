package org.netbeans.gradle.project.script;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.netbeans.gradle.project.util.NbFileUtils;

public final class DefaultScriptFileProvider implements ScriptFileProvider {
    private static final String[] EXTENSIONS = {".gradle", ".gradle.kts"};

    public DefaultScriptFileProvider() {
    }

    @Override
    public boolean isScriptFileName(String fileName) {
        String normName = fileName.toLowerCase(Locale.ROOT);
        for (String ext: EXTENSIONS) {
            if (normName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Path findScriptFile(Path baseDir, String baseName) {
        for (String ext: EXTENSIONS) {
            Path candidate = baseDir.resolve(baseName + ext);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public Iterable<Path> findScriptFiles(Path baseDir, String baseName) {
        List<Path> result = new ArrayList<>(EXTENSIONS.length);
        for (String ext: EXTENSIONS) {
            Path candidate = baseDir.resolve(baseName + ext);
            if (Files.isRegularFile(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    @Override
    public Collection<Path> findScriptFiles(
            Path baseDir,
            Predicate<? super String> baseNameFilter) throws IOException {

        List<Path> result = new ArrayList<>();
        findScriptFiles(baseDir, baseNameFilter, result::add);
        return result;
    }

    @Override
    public void findScriptFiles(
            Path baseDir,
            Predicate<? super String> baseNameFilter,
            Consumer<Path> fileProcessor) throws IOException {
        Objects.requireNonNull(baseDir, "baseDir");
        Objects.requireNonNull(baseNameFilter, "baseNameFilter");
        Objects.requireNonNull(fileProcessor, "fileProcessor");

        try (DirectoryStream<Path> files = Files.newDirectoryStream(baseDir)) {
            for (Path candidate: files) {
                String name = NbFileUtils.getFileNameStr(candidate);
                String normName = name.toLowerCase(Locale.ROOT);
                for (String ext: EXTENSIONS) {
                    if (normName.endsWith(ext)) {
                        String baseName = name.substring(0, name.length() - ext.length());
                        if (baseNameFilter.test(baseName)) {
                            fileProcessor.accept(candidate);
                        }
                        break;
                    }
                }
            }
        } catch (NotDirectoryException ex) {
            // Ignore, no files are returned
        }
    }
}
