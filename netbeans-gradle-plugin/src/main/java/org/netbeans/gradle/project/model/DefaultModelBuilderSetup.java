package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.gradle.tooling.ProgressListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.BuildOperationArgs;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.standard.JavaPlatformUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;

public final class DefaultModelBuilderSetup implements OperationInitializer {
    private static final SpecificationVersion DEFAULT_JDK_VERSION = new SpecificationVersion("1.5");

    private final ProgressHandle progress;
    private final JavaPlatform jdkPlatform;
    private final File jdkHome;
    private final List<String> arguments;
    private final List<String> jvmArgs;

    public DefaultModelBuilderSetup(Project project, List<String> arguments, List<String> jvmArgs, ProgressHandle progress) {
        this.progress = progress;
        JavaPlatform selectedPlatform = tryGetScriptJavaPlatform(project);
        this.jdkHome = getScriptJavaHome(selectedPlatform);
        this.jdkPlatform = selectedPlatform != null ? selectedPlatform : JavaPlatform.getDefault();
        this.arguments = arguments != null ? new ArrayList<>(arguments) : Collections.<String>emptyList();
        this.jvmArgs = jvmArgs != null ? new ArrayList<>(jvmArgs) : Collections.<String>emptyList();
    }

    public JavaPlatform getJdkPlatform() {
        return jdkPlatform;
    }

    public SpecificationVersion getJDKVersion() {
        Specification spec = jdkPlatform.getSpecification();
        if (spec == null) {
            return DEFAULT_JDK_VERSION;
        }
        SpecificationVersion result = spec.getVersion();
        return result != null ? result : DEFAULT_JDK_VERSION;
    }

    @Override
    public void initOperation(BuildOperationArgs args) {
        if (jdkHome != null && !jdkHome.getPath().isEmpty()) {
            args.setJavaHome(jdkHome);
        }
        if (!arguments.isEmpty()) {
            args.setArguments(arguments.toArray(new String[arguments.size()]));
        }
        if (!jvmArgs.isEmpty()) {
            args.setJvmArguments(jvmArgs.toArray(new String[jvmArgs.size()]));
        }
        if (progress != null) {
            args.setProgressListeners(new ProgressListener[]{pe -> {
                progress.progress(pe.getDescription());
            }});
        }
    }

    private static File getScriptJavaHome(JavaPlatform platform) {
        FileObject jdkHomeObj = platform != null ? JavaPlatformUtils.getHomeFolder(platform) : null;
        if (jdkHomeObj != null) {
            // This is necessary for unit test code because JavaPlatform returns
            // the jre inside the JDK.
            if ("jre".equals(jdkHomeObj.getNameExt().toLowerCase(Locale.ROOT))) {
                FileObject parent = jdkHomeObj.getParent();
                if (parent != null) {
                    jdkHomeObj = parent;
                }
            }
        }
        return jdkHomeObj != null ? FileUtil.toFile(jdkHomeObj) : null;
    }

    private static JavaPlatform tryGetScriptJavaPlatform(Project project) {
        Objects.requireNonNull(project, "project");

        NbGradleProject gradleProject = NbGradleProjectFactory.tryGetGradleProject(project);
        ScriptPlatform result = gradleProject != null ? gradleProject.getCommonProperties().scriptPlatform().getActiveValue() : null;
        return result != null ? result.getJavaPlatform() : null;
    }

}
