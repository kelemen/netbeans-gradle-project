package org.netbeans.gradle.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.file.AbstractFileCollection;

public final class ZippedJarsFileCollection extends AbstractFileCollection {
    private final Charset UTF8 = Charset.forName("UTF-8");

    private final Project project;
    private final String dependencyNotation;
    private final Configuration dependencyContainer;
    private final Path unzipDir;
    private final List<String> subDirPath;

    public ZippedJarsFileCollection(
            Project project,
            String dependencyNotation,
            List<String> unzipPath,
            List<String> subDirPath) {
        this.project = project;
        Dependency dependency = project.getDependencies().create(dependencyNotation);
        this.dependencyContainer = project.getConfigurations().detachedConfiguration(dependency);
        this.unzipDir = resolve(project.getProjectDir().toPath(), unzipPath);
        this.dependencyNotation = dependencyNotation;
        this.subDirPath = new ArrayList<>(subDirPath);
    }

    private static Path resolve(Path dir, List<String> subDirs) {
        Path result = dir;
        for (String subDir: subDirs) {
            result = result.resolve(subDir);
        }
        return result;
    }

    private Path getExtractedFilesDir() {
        return unzipDir.resolve("files");
    }

    private Path getExtractedMarker() {
        return unzipDir.resolve("EXTRACTED");
    }

    private void extractSafe() {
        try {
            extract();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void safeCopy(Path src, Path dest) throws IOException {
        try (InputStream input = Files.newInputStream(src)) {
            Files.copy(input, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void markExtracted() throws IOException {
        Path extractedMarker = getExtractedMarker();
        List<String> lines = Collections.singletonList(dependencyNotation);
        Files.write(extractedMarker, lines, UTF8);
    }

    private boolean isExtracted() throws IOException {
        Path extractedMarker = getExtractedMarker();
        if (!Files.isRegularFile(extractedMarker)) {
            return false;
        }

        List<String> lines = Files.readAllLines(extractedMarker, UTF8);
        if (lines.isEmpty()) {
            return false;
        }

        return dependencyNotation.equals(lines.get(0));
    }

    private void tryDeleteDirectory(Path directory) {
        project.delete(directory.toFile());
    }

    private void copyDir(final Path srcDir, final Path destDir) throws IOException {
        tryDeleteDirectory(destDir);
        Files.createDirectories(destDir);

        Files.walkFileTree(srcDir, new FileVisitor<Path>() {
            private Path currentDir = destDir;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!srcDir.equals(dir)) {
                    currentDir = currentDir.resolve(dir.getFileName().toString());
                    Files.createDirectory(currentDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String normName = fileName.toLowerCase(Locale.ROOT);
                if (normName.endsWith(".jar")) {
                    safeCopy(file, currentDir.resolve(fileName));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                currentDir = currentDir != null ? currentDir.getParent() : null;
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void extract() throws IOException {
        if (isExtracted()) {
            return;
        }

        Path zipFile = dependencyContainer.getSingleFile().toPath();
        FileSystem zipFS = FileSystems.newFileSystem(zipFile, null);
        Path root = zipFS.getRootDirectories().iterator().next();
        Path libDir = resolve(root, subDirPath);

        copyDir(libDir, getExtractedFilesDir());

        markExtracted();
    }

    @Override
    public String getDisplayName() {
        return "UnZip: " + dependencyNotation;
    }

    @Override
    public Set<File> getFiles() {
        extractSafe();

        return project.fileTree(getExtractedFilesDir().toFile()).getFiles();
    }
}
