package org.netbeans.gradle.project.newproject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.StringUtils;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

@TemplateRegistration(
        folder="Project/Gradle",
        displayName="#template.singleGradleProject",
        iconBase=NbIcons.PROJECT_ICON_PATH)
@Messages("template.singleGradleProject=Single Gradle Project")
public final class GradleSingleProjectWizardIterator
implements
        WizardDescriptor.BackgroundInstantiatingIterator<WizardDescriptor> {

    @StaticResource
    private static final String SINGLE_PROJECT_BUILD_GRADLE = "org/netbeans/gradle/project/resources/newproject/single-project.gradle";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final List<WizardDescriptor.Panel<WizardDescriptor>> descriptors;
    private final AtomicReference<GradleSingleProjectConfig> configRef;
    private int descriptorIndex;

    public GradleSingleProjectWizardIterator() {
        this.descriptors = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>(1);
        this.descriptorIndex = 0;
        this.configRef = new AtomicReference<GradleSingleProjectConfig>(null);
    }

    private static void createBuildGradle(
            File projectDir,
            GradleSingleProjectConfig config) throws IOException {
        String mainClass = config.getMainClass();

        String buildGradleContent = StringUtils.getResourceAsString(SINGLE_PROJECT_BUILD_GRADLE, UTF8);

        buildGradleContent = buildGradleContent.replace("${MAIN_CLASS}",
                mainClass != null ? mainClass : "");
        buildGradleContent = buildGradleContent.replace("${SOURCE_LEVEL}",
                AbstractProjectProperties.getSourceLevelFromPlatform(JavaPlatform.getDefault()));

        File buildGradle = new File(projectDir, GradleProjectConstants.BUILD_FILE_NAME);
        StringUtils.writeStringToFile(buildGradleContent, UTF8, buildGradle);
    }

    private static void createMainClass(
            File projectDir,
            GradleSingleProjectConfig config) throws IOException {
        String mainClass = config.getMainClass();
        if (mainClass == null) {
            return;
        }

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
        StringUtils.writeStringToFile(content.toString(), UTF8, mainClassPath);
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        GradleSingleProjectConfig config = configRef.get();
        if (config == null) {
            throw new IOException("Missing configuration.");
        }

        File projectDirAsFile = FileUtil.normalizeFile(config.getProjectFolder());
        FileObject projectDir = FileUtil.createFolder(projectDirAsFile);

        GradleFileUtils.createDefaultSourceDirs(projectDir);
        createBuildGradle(projectDirAsFile, config);
        createMainClass(projectDirAsFile, config);
        StringUtils.writeStringToFile("", UTF8, new File(projectDirAsFile, GradleProjectConstants.SETTINGS_FILE_NAME));

        return Collections.singleton(projectDir);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        uninitialize(wizard);

        descriptorIndex = 0;
        descriptors.add(new GradleSingleProjectConfigPanel(configRef));
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        descriptors.clear();
        configRef.set(null);
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return descriptors.get(descriptorIndex);
    }

    @Override
    public String name() {
        return "GradleSingleProjectTemplate";
    }

    @Override
    public boolean hasNext() {
        return descriptorIndex < descriptors.size() - 1;
    }

    @Override
    public boolean hasPrevious() {
        return descriptorIndex > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        descriptorIndex++;
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        descriptorIndex--;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }
}
