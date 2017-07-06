package org.netbeans.gradle.build;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.maven.shared.dependency.analyzer.asm.ASMDependencyAnalyzer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.jvm.tasks.Jar;

public final class NbmDependencyVerifierPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        Task verifyTask = project.task("verifyModuleDependencies");
        verifyTask.dependsOn("jar");
        project.getTasks().getByName("check").dependsOn(verifyTask);

        verifyTask.doLast(task -> {
            Jar jar = (Jar)project.getTasks().getByName("jar");
            verifyDependencies(project, jar.getArchivePath());
        });
    }

    private void verifyDependencies(Project project, File jarPath) {
        try {
            verifyDependencies0(project, jarPath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Set<String> getOwnClasses(File jarPath) throws IOException {
        final Set<String> result = new HashSet<>();
        try (FileSystem fs = FileSystems.newFileSystem(jarPath.toPath(), null)) {
            for (Path root: fs.getRootDirectories()) {
                Files.walkFileTree(root, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String extension = ".class";
                        String path = file.toString();
                        if (path.toLowerCase(Locale.ROOT).endsWith(extension)) {
                            String extensionLessPath = path.substring(0, path.length() - extension.length());
                            String className = removeStartingChars(extensionLessPath.replace('/', '.'), '.');
                            result.add(className);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        return result;
    }

    private static String removeStartingChars(String str, char ch) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ch) {
                return str.substring(i);
            }
        }
        return "";
    }

    private void verifyDependencies0(Project project, File jarPath) throws IOException {
        Set<String> ownClasses = getOwnClasses(jarPath);
        Set<String> classes = getClasses(jarPath);

        Configuration providedCompile = project.getConfigurations().getByName("providedCompile");
        FileCollection compile = project.getConfigurations().getByName("compile").minus(providedCompile);

        Map<String, String> missing = new HashMap<>();
        try (URLClassLoader accessibleLoader = urlClassLoader(getFirstLevelUrls(providedCompile));
                URLClassLoader allClassLoader = urlClassLoader(getAllUrls(providedCompile));
                URLClassLoader compileLoader = urlClassLoader(getAllUrls(compile))) {

            for (String className: classes) {
                if (!ownClasses.contains(className) && !hasClass(accessibleLoader, className)) {
                    if (hasClass(allClassLoader, className) && !hasClass(compileLoader, className)) {
                        URL owner = tryGetClassOwner(allClassLoader, className);
                        missing.put(className, owner != null ? owner.toString() : "unknown");
                    }
                }
            }
        }

        if (!missing.isEmpty()) {
            failWithMissingDependency(missing);
        }
    }

    private static URLClassLoader urlClassLoader(List<URL> urls) {
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader());
    }

    private static URL tryGetClassOwner(ClassLoader loader, String className) {
        Class<?> cl;
        try {
            cl = loader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            return null;
        }

        CodeSource codeSource = cl.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }
        return codeSource.getLocation();
    }

    private static boolean hasClass(ClassLoader loader, String className) {
        return loader.getResource(className.replace('.', '/') + ".class") != null;
    }

    private void failWithMissingDependency(Map<String, String> missing) {
        List<String> sortedMissing = new ArrayList<>(missing.keySet());
        Collections.sort(sortedMissing);

        StringBuilder message = new StringBuilder();
        message.append("The following classes are not in a directly declared dependency:");
        for (String dependency: sortedMissing) {
            message.append("\n- ");
            message.append(dependency);
            message.append(" but was found in ");
            message.append(missing.get(dependency));
        }
        message.append("\n\nRequired explicit dependencies:");

        Set<String> result = new TreeSet<>(missing.values());

        for (String owner: result) {
            message.append("\n- ");
            message.append(owner);
        }
        message.append("\n");

        throw new IllegalStateException(message.toString());
    }

    private List<URL> getAllUrls(FileCollection config) throws MalformedURLException {
        List<URL> result = new ArrayList<>();
        for (File dep: config.getFiles()) {
            result.add(dep.toURI().toURL());
        }
        return result;
    }

    private List<URL> getFirstLevelUrls(Configuration config) throws MalformedURLException {
        List<URL> result = new ArrayList<>();
        for (ResolvedDependency dep: config.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
            for (ResolvedArtifact art: dep.getModuleArtifacts()) {
                result.add(art.getFile().toURI().toURL());
            }
        }
        return result;
    }

    private Set<String> getClasses(File jarPath) throws IOException {
        ASMDependencyAnalyzer analizer = new ASMDependencyAnalyzer();
        return analizer.analyze(jarPath.toURI().toURL());
    }
}
