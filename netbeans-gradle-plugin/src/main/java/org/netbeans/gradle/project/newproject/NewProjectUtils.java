package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.text.JTextComponent;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.validate.BackgroundValidator;
import org.netbeans.gradle.project.validate.Problem;
import org.netbeans.gradle.project.validate.Validator;
import org.netbeans.gradle.project.validate.Validators;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

import static org.netbeans.gradle.project.validate.Validators.*;

public final class NewProjectUtils {
    public static final Charset DEFAULT_FILE_ENCODING = StringUtils.UTF8;

    private static final Pattern RECOMMENDED_PROJECTNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-.]*[a-zA-Z0-9]");
    private static final Pattern MAVEN_GROUP_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-.]*[a-zA-Z0-9]");
    private static final Pattern MAVEN_VERSION_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-.]*[a-zA-Z0-9]");

    public static Preferences getPreferences() {
        return NbPreferences.forModule(NewProjectUtils.class);
    }

    public static void copyTemplateFile(
            String resourcePath,
            Path destination,
            Charset encoding,
            Map<String, String> varReplaceMap) throws IOException {

        String resourceContent = StringUtils.getResourceAsString(resourcePath, encoding);

        for (Map.Entry<String, String> entry: varReplaceMap.entrySet()) {
            resourceContent = resourceContent.replace(entry.getKey(), entry.getValue());
        }

        resourceContent = StringUtils.replaceLFWithPreferredLineSeparator(resourceContent);
        StringUtils.writeStringToFile(resourceContent, encoding, destination);
    }

    public static String getDefaultProjectDir(WizardDescriptor settings) {
        return ProjectChooser.getProjectsFolder().getAbsolutePath();
    }

    public static void setDefaultProjectDir(String newValue) {
        File dir = new File(newValue.trim());
        if (dir.isDirectory()) {
            ProjectChooser.setProjectsFolder(dir);
        }
    }

    public static Path resolveAndCreateDir(Path base, String dirName) throws IOException {
        Path result = base.resolve(dirName);
        Files.createDirectory(result);
        return result;
    }

    public static void createDefaultSourceDirs(Path projectDir) throws IOException {
        Path srcDir = resolveAndCreateDir(projectDir, "src");
        Path mainDir = resolveAndCreateDir(srcDir, "main");
        Path testDir = resolveAndCreateDir(srcDir, "test");

        resolveAndCreateDir(mainDir, "java");
        resolveAndCreateDir(mainDir, "resources");

        resolveAndCreateDir(testDir, "java");
        resolveAndCreateDir(testDir, "resources");
    }

    public static void createMainClass(Path projectDir, String mainClass) throws IOException {
        Objects.requireNonNull(projectDir, "projectDir");
        Objects.requireNonNull(mainClass, "mainClass");

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

        String separator = projectDir.getFileSystem().getSeparator();
        String relPackagePathStr = packageName.replace(".", separator);

        Path packagePath = projectDir.resolve("src").resolve("main").resolve("java");
        if (!relPackagePathStr.isEmpty()) {
            packagePath = packagePath.resolve(relPackagePathStr);
        }

        List<String> content = new ArrayList<>();
        if (!packageName.isEmpty()) {
            content.add("package " + packageName + ";");
            content.add("");
        }

        content.add("public class " + simpleClassName + " {");
        content.add("    /**");
        content.add("     * @param args the command line arguments");
        content.add("     */");
        content.add("    public static void main(String[] args) {");
        content.add("    }");
        content.add("}");

        Path mainClassPath = packagePath.resolve(simpleClassName + ".java");

        Files.createDirectories(packagePath);
        NbFileUtils.writeLinesToFile(mainClassPath, content, DEFAULT_FILE_ENCODING);
    }

    public static Validator<String> createProjectNameValidator() {
        Validator<String> patternValidators = merge(
                createFileNameValidator(
                    Problem.Level.SEVERE,
                    NewProjectStrings.getIllegalProjectName()),
                createPatternValidator(RECOMMENDED_PROJECTNAME_PATTERN,
                    Problem.Level.WARNING,
                    NewProjectStrings.getNotRecommendedProjectName()));
        Validator<String> notEmptyValidator = (String inputType) -> {
            return inputType.isEmpty()
                    ? Problem.severe(NewProjectStrings.getProjectNameMustNotBeEmpty())
                    : null;
        };

        return merge(notEmptyValidator, patternValidators);
    }

    public static Validator<String> createGroupIdValidator() {
        return createPatternValidator(
                MAVEN_GROUP_ID_PATTERN,
                Problem.Level.SEVERE,
                NewProjectStrings.getInvalidGroupId());
    }

    public static Validator<String> createVersionValidator() {
        return createPatternValidator(
                MAVEN_VERSION_PATTERN,
                Problem.Level.SEVERE,
                NewProjectStrings.getInvalidVersion());
    }

    public static Validator<String> createNewFolderValidator() {
        return (String inputType) -> {
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
        };
    }

    public static Validator<String> createVariableNameValidator() {
        return (String inputType) -> {
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
        };
    }

    public static Validator<String> createClassNameValidator(final boolean optional) {
        Validator<String> varNameValidator = createVariableNameValidator();

        return (String inputType) -> {
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
        };
    }

    public static void chooseProjectLocation(Component parent, JTextComponent jProjectEdit) {
        Objects.requireNonNull(jProjectEdit, "jProjectEdit");

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(null);
        chooser.setDialogTitle(NbStrings.getSelectProjectLocationCaption());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        String path = jProjectEdit.getText();
        if (!path.isEmpty()) {
            File initialSelection = new File(path);
            if (initialSelection.exists()) {
                chooser.setSelectedFile(initialSelection);
            }
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(parent)) {
            File projectDir = chooser.getSelectedFile();
            jProjectEdit.setText(FileUtil.normalizeFile(projectDir).getAbsolutePath());
        }
    }

    public static ListenerRef setupNewProjectValidators(
            final BackgroundValidator bckgValidator,
            final JTextComponent jProjectNameEdit,
            final JTextComponent jProjectFolderEdit,
            final JTextComponent jProjectLocationEdit) {
        Objects.requireNonNull(bckgValidator, "bckgValidator");
        Objects.requireNonNull(jProjectNameEdit, "jProjectNameEdit");
        Objects.requireNonNull(jProjectFolderEdit, "jProjectFolderEdit");
        Objects.requireNonNull(jProjectLocationEdit, "jProjectLocationEdit");

        PropertySource<String> projectName = Validators.trimmedText(jProjectNameEdit);
        PropertySource<String> projectFolder = Validators.trimmedText(jProjectFolderEdit);
        PropertySource<String> projectLocation = Validators.trimmedText(jProjectLocationEdit);

        List<ListenerRef> refs = new ArrayList<>();

        refs.add(bckgValidator.addValidator(
                NewProjectUtils.createProjectNameValidator(),
                projectName));
        refs.add(bckgValidator.addValidator(
                NewProjectUtils.createNewFolderValidator(),
                projectFolder));

        Runnable projectFolderUpdater = () -> {
            File location = new File(
                    jProjectLocationEdit.getText().trim(),
                    jProjectNameEdit.getText().trim());
            jProjectFolderEdit.setText(location.getPath());
        };

        refs.add(projectName.addChangeListener(projectFolderUpdater));
        refs.add(projectLocation.addChangeListener(projectFolderUpdater));

        return ListenerRefs.combineListenerRefs(refs);
    }

    private NewProjectUtils() {
        throw new AssertionError();
    }
}
