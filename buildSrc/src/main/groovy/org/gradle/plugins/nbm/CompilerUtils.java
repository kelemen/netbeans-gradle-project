package org.gradle.plugins.nbm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.compile.JavaCompile;

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

    private CompilerUtils() {
        throw new AssertionError();
    }

}
