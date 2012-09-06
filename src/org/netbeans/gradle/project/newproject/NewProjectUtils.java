package org.netbeans.gradle.project.newproject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import javax.swing.text.JTextComponent;
import org.netbeans.gradle.project.StringUtils;
import org.netbeans.gradle.project.validate.InputCollector;
import org.netbeans.gradle.project.validate.Problem;
import org.netbeans.gradle.project.validate.Validator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

public final class NewProjectUtils {
    public static final Charset DEFAULT_FILE_ENCODING = Charset.forName("UTF-8");

    private static final Pattern LEGAL_FILENAME_PATTERN = Pattern.compile("[^/./\\:*?\"<>|]*");
    private static final Pattern RECOMMENDED_PROJECTNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-.]*");

    public static void createDefaultSourceDirs(FileObject projectDir) throws IOException {
        FileObject srcDir = projectDir.createFolder("src");
        FileObject mainDir = srcDir.createFolder("main");
        FileObject testDir = srcDir.createFolder("test");

        mainDir.createFolder("java");
        mainDir.createFolder("resources");

        testDir.createFolder("java");
        testDir.createFolder("resources");
    }

    public static void createMainClass(File projectDir, String mainClass) throws IOException {
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (mainClass == null) throw new NullPointerException("mainClass");

        String packageName;
        String simpleClassName;

        int classSepIndex = mainClass.lastIndexOf('.');
        if (classSepIndex >= 0) {
            packageName = mainClass.substring(0, classSepIndex);
            simpleClassName = mainClass.substring(classSepIndex + 1);
        }
        else {
            packageName = "";
            simpleClassName = mainClass;
        }

        String relPackagePathStr = packageName.replace(".", File.separator);

        File packagePath = new File(projectDir, "src");
        packagePath = new File(packagePath, "main");
        packagePath = new File(packagePath, "java");
        if (!relPackagePathStr.isEmpty()) {
            packagePath = new File(packagePath, relPackagePathStr);
        }

        StringBuilder content = new StringBuilder(256);
        if (!packageName.isEmpty()) {
            content.append("package ");
            content.append(packageName);
            content.append(";\n\n");
        }

        content.append("public class ");
        content.append(simpleClassName);
        content.append(" {\n");
        content.append("    /**\n");
        content.append("     * @param args the command line arguments\n");
        content.append("     */\n");
        content.append("    public static void main(String[] args) {\n");
        content.append("    }\n");
        content.append("}\n");

        File mainClassPath = new File(packagePath, simpleClassName + ".java");

        FileUtil.createFolder(packagePath);
        StringUtils.writeStringToFile(content.toString(), DEFAULT_FILE_ENCODING, mainClassPath);
    }

    public static <InputType> Validator<InputType> merge(
            final Validator<? super InputType> validator1,
            final Validator<? super InputType> validator2) {

        return new Validator<InputType>() {
            @Override
            public Problem validateInput(InputType inputType) {
                Problem problem1 = validator1.validateInput(inputType);
                Problem problem2 = validator2.validateInput(inputType);

                if (problem1 == null) {
                    return problem2;
                }
                if (problem2 == null) {
                    return problem1;
                }

                return problem1.getLevel().getIntValue() >= problem2.getLevel().getIntValue()
                        ? problem1
                        : problem2;
            }
        };
    }

    public static Validator<String> createPatternValidator(
            final Pattern pattern,
            final Problem.Level severity,
            final String errorMessage) {
        if (pattern == null) throw new NullPointerException("pattern");
        if (severity == null) throw new NullPointerException("severity");
        if (errorMessage == null) throw new NullPointerException("errorMessage");

        return new Validator<String>() {
            @Override
            public Problem validateInput(String inputType) {
                if (!pattern.matcher(inputType).matches()) {
                    return new Problem(severity, errorMessage);
                }
                return null;
            }
        };
    }

    public static Validator<String> createProjectNameValidator() {
        Validator<String> patternValidators = merge(
                createPatternValidator(LEGAL_FILENAME_PATTERN,
                    Problem.Level.SEVERE,
                    NewProjectStrings.getIllegalProjectName()),
                createPatternValidator(RECOMMENDED_PROJECTNAME_PATTERN,
                    Problem.Level.WARNING,
                    NewProjectStrings.getNotRecommendedProjectName()));
        Validator<String> notEmptyValidator = new Validator<String>() {
            @Override
            public Problem validateInput(String inputType) {
                return inputType.isEmpty()
                        ? Problem.severe(NewProjectStrings.getProjectNameMustNotBeEmpty())
                        : null;
            }
        };

        return merge(notEmptyValidator, patternValidators);
    }

    public static Validator<String> createNewFolderValidator() {
        return new Validator<String>() {
            @Override
            public Problem validateInput(String inputType) {
                String projectDirStr = inputType.trim();
                if (projectDirStr.isEmpty()) {
                    return Problem.severe(NewProjectStrings.getInvalidPath());
                }

                File projectDir = FileUtil.normalizeFile(new File(projectDirStr));
                if (projectDir == null) {
                    return Problem.severe(NewProjectStrings.getInvalidPath());
                }

                // This check is required because checking these kind of paths
                // can be extremly slow.
                if (Utilities.isWindows() && projectDir.getAbsolutePath().startsWith("\\\\")) {
                    return Problem.severe(NewProjectStrings.getCannotCreateFolderHere());
                }

                if (projectDir.exists()) {
                    return Problem.severe(NewProjectStrings.getDirectoryAlreadyExists());
                }

                File rootPath = projectDir;
                while (rootPath != null && !rootPath.exists()) {
                    rootPath = rootPath.getParentFile();
                }

                if (rootPath == null || !rootPath.canWrite() || !rootPath.isDirectory()) {
                    return Problem.severe(NewProjectStrings.getCannotCreateFolderHere());
                }
                return null;
            }
        };
    }

    public static Validator<String> createVariableNameValidator() {
        return new Validator<String>() {
            @Override
            public Problem validateInput(String inputType) {
                if (inputType.isEmpty()) {
                    return Problem.severe(NewProjectStrings.getIllegalIdentifier());
                }

                if (!Character.isJavaIdentifierStart(inputType.charAt(0))) {
                    return Problem.severe(NewProjectStrings.getIllegalIdentifier());
                }

                for (int i = 1; i < inputType.length(); i++) {
                    if (!Character.isJavaIdentifierPart(inputType.charAt(i))) {
                        return Problem.severe(NewProjectStrings.getIllegalIdentifier());
                    }
                }
                return null;
            }
        };
    }

    public static Validator<String> createClassNameValidator(final boolean optional) {
        final Validator<String> varNameValidator = createVariableNameValidator();

        return new Validator<String>() {
            @Override
            public Problem validateInput(String inputType) {
                if (optional && inputType.isEmpty()) {
                    return null;
                }

                if (inputType.endsWith(".")) {
                    return Problem.severe(NewProjectStrings.getIllegalIdentifier());
                }

                String[] parts = inputType.split(Pattern.quote("."));
                for (String part: parts) {
                    Problem problem = varNameValidator.validateInput(part);
                    if (problem != null) {
                        assert problem.getLevel() == Problem.Level.SEVERE;
                        return problem;
                    }
                }
                if (parts.length == 1) {
                    return Problem.warning(NewProjectStrings.getShouldNotUseDefaultPackage());
                }

                return null;
            }
        };
    }

    public static InputCollector<String> createCollector(
            final JTextComponent component) {
        if (component == null) throw new NullPointerException("component");

        return new InputCollector<String>() {
            @Override
            public String getInput() {
                String result = component.getText();
                return result != null ? result.trim() : "";
            }
        };
    }

    private NewProjectUtils() {
        throw new AssertionError();
    }
}
