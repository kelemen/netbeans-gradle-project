package org.gradle.plugins.nbm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.netbeans.gradle.build.TaskConfigurations;

public final class CompilerUtils {
    private static final String JAVAC_VERSION_PREFIX = "javac";

    private static String tryGetCompilerVersion(ForkOptions forkOptions) {
        String executable = forkOptions.getExecutable();
        if (executable == null || executable.isEmpty()) {
            return null;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(executable, "-version");
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        try {
            Process process = processBuilder.start();

            InputStream input = process.getErrorStream();
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input, "ISO-8859-1"), 4096);

            String result = null;
            String line = inputReader.readLine();
            while (line != null) {
                if (line.startsWith(JAVAC_VERSION_PREFIX)) {
                    result = line.substring(JAVAC_VERSION_PREFIX.length()).trim();
                    // Continue reading to prevent dead-locking the process if it
                    // prints something else.
                }
                line = inputReader.readLine();
            }
            return result;
        } catch (IOException ex) {
            return null;
        }
    }

    public static JavaVersion getTargetCompatibility(Project project) {
        JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
        return javaPlugin.getTargetCompatibility();
    }

    public static File findToolsJar(Project project, JavaVersion javaVersion) {
        String explicitToolsJarProperty = "jdk" + javaVersion.getMajorVersion() + "ToolsJar";
        if (project.hasProperty(explicitToolsJarProperty)) {
            return new File(project.property(explicitToolsJarProperty).toString().trim());
        }

        String foundToolsJar = null;
        String explicitJavaCompiler = tryGetExplicitJdkCompiler(project, javaVersion);
        if (explicitJavaCompiler != null) {
            foundToolsJar = extractToolsJarFromCompiler(explicitJavaCompiler);
        }

        if (foundToolsJar == null) {
            String javaHome = System.getProperty("java.home");
            foundToolsJar = extractToolsJarFromJavaHome(javaHome);
        }

        if (foundToolsJar == null) {
            throw new IllegalStateException("Unable to find the JDK's tools.jar.");
        }

        return new File(foundToolsJar);
    }

    public static String tryGetCompilerVersion(JavaCompile compileTask) {
        CompileOptions options = compileTask.getOptions();
        if (options.isFork()) {
            ForkOptions forkOptions = options.getForkOptions();
            return tryGetCompilerVersion(forkOptions);
        }
        else {
            return System.getProperty("java.version");
        }
    }

    public static void configureJavaCompilers(Project project) {
        project.getTasks().withType(JavaCompile.class, new Action<JavaCompile>() {
            @Override
            public void execute(JavaCompile compileTask) {
                configureJavaCompiler(compileTask);
            }
        });
    }

    private static void configureJavaCompiler(final JavaCompile compileTask) {
        CompileOptions options = compileTask.getOptions();
        options.setEncoding("UTF-8");
        options.setCompilerArgs(Arrays.asList("-Xlint"));

        TaskConfigurations.lazilyConfiguredTask(compileTask, new Action<Task>() {
            @Override
            public void execute(Task task) {
                configureJavacNow(compileTask);
            }
        });
    }

    private static void configureJavacNow(JavaCompile compileTask) {
        final JavaVersion targetCompatibility = getTargetCompatibility(compileTask.getProject());
        if (Objects.equals(JavaVersion.current(), targetCompatibility)) {
            return;
        }

        final Project project = compileTask.getProject();
        String explicitJavaCompiler = tryGetExplicitJdkCompiler(project, targetCompatibility);
        if (explicitJavaCompiler != null) {
            CompileOptions compilerOptions = compileTask.getOptions();
            compilerOptions.setFork(true);
            compilerOptions.getForkOptions().setExecutable(explicitJavaCompiler);
        }
        else {
            compileTask.doFirst(new Action<Task>() {
                @Override
                public void execute(Task t) {
                    String jdkProperty = getJdkPropertyName(targetCompatibility);
                    project.getLogger().warn("Warning: " + jdkProperty + " property is missing and"
                            + " not compiling with Java " + targetCompatibility
                            + ". Using " + JavaVersion.current());
                }
            });
        }
    }

    private static Path subPath(Path base, String... subPaths) {
        if (base == null) {
            return null;
        }

        Path result = base;
        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    private static Path getParent(Path base, int level) {
        Path result = base;
        for (int i = 0; i < level && result != null; i++) {
            result = result.getParent();
        }
        return result;
    }

    private static String extractToolsJarFromJDKHome(Path jdkHome) {
        Path toolsJar = subPath(jdkHome, "lib", "tools.jar");
        if (toolsJar == null) {
            return null;
        }

        if (!Files.isRegularFile(toolsJar)) {
            return null;
        }

        return toolsJar.toString();
    }

    private static String extractToolsJarFromJavaHome(String javaHome) {
        if (javaHome == null) {
            return null;
        }

        Path jdkHome = Paths.get(javaHome).getParent();
        return extractToolsJarFromJDKHome(jdkHome);
    }

    private static String extractToolsJarFromCompiler(String javac) {
        if (javac == null) {
            return null;
        }

        Path jdkHome = getParent(Paths.get(javac), 2);
        return extractToolsJarFromJDKHome(jdkHome);
    }

    private static String getJdkPropertyName(JavaVersion javaVersion) {
        return "jdk" + javaVersion.getMajorVersion() + "Compiler";
    }

    private static String tryGetExplicitJdkCompiler(Project project, JavaVersion javaVersion) {
        String jdkProperty = getJdkPropertyName(javaVersion);
        if (project.hasProperty(jdkProperty)) {
            return project.property(jdkProperty).toString().trim();
        }
        else {
            return null;
        }
    }

    private CompilerUtils() {
        throw new AssertionError();
    }

}
