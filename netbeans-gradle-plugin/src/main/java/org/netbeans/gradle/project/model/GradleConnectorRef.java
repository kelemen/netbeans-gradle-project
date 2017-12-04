package org.netbeans.gradle.project.model;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationDefault;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.openide.util.Lookup;

public final class GradleConnectorRef {
    private final GradleConnector gradleConnector;
    private final GradleLocation requestedGradleLocation;
    private final File requestedGradleUserHome;

    private GradleConnectorRef(
            GradleConnector gradleConnector,
            GradleLocation requestedGradleLocation,
            File requestedGradleUserHome) {

        this.gradleConnector = Objects.requireNonNull(gradleConnector, "gradleConnector");
        this.requestedGradleLocation = Objects.requireNonNull(requestedGradleLocation, "requestedGradleLocation");
        this.requestedGradleUserHome = requestedGradleUserHome;
    }

    public static GradleConnectorRef open(CancellationToken cancelToken, Project project) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(project, "project");

        final GradleConnector result = GradleConnector.newConnector();
        Integer timeoutSec = CommonGlobalSettings.getDefault().gradleDaemonTimeoutSec().getActiveValue();
        if (timeoutSec != null && result instanceof DefaultGradleConnector) {
            ((DefaultGradleConnector)result).daemonMaxIdleTime(timeoutSec, TimeUnit.SECONDS);
        }

        NbGradleProject gradleProject = NbGradleProjectFactory.getGradleProject(project);

        File gradleUserHome = CommonGlobalSettings.getDefault().gradleUserHomeDir().getActiveValue();
        if (gradleUserHome != null) {
            result.useGradleUserHomeDir(gradleUserHome);
        }

        GradleLocation gradleLocation = getGradleLocation(gradleProject);
        gradleLocation.applyLocation(new GradleLocation.Applier() {
            @Override
            public void applyVersion(String versionStr) {
                result.useGradleVersion(versionStr);
            }

            @Override
            public void applyDirectory(File gradleHome) {
                result.useInstallation(gradleHome);
            }

            @Override
            public void applyDistribution(URI location) {
                result.useDistribution(location);
            }

            @Override
            public void applyDefault() {
            }
        });

        return new GradleConnectorRef(result, gradleLocation, gradleUserHome);
    }

    private static GradleLocation getGradleLocation(NbGradleProject gradleProject) {
        NbGradleCommonProperties commonProperties = gradleProject.getCommonProperties();
        GradleLocationDef gradleLocationDef = commonProperties.gradleLocation().getActiveValue();

        if (shouldRelyOnWrapper(gradleProject, gradleLocationDef)) {
            return GradleLocationDefault.DEFAULT;
        }

        StringResolver resolver = StringResolvers.getDefaultResolverSelector().getProjectResolver(gradleProject, Lookup.EMPTY);
        return gradleLocationDef.getLocation(resolver);
    }

    public GradleLocation getRequestedGradleLocation() {
        return requestedGradleLocation;
    }

    public File getRequestedGradleUserHome() {
        return requestedGradleUserHome;
    }

    private static boolean shouldRelyOnWrapper(NbGradleProject project, GradleLocationDef locationDef) {
        if (locationDef.getLocationRef() == GradleLocationDefault.DEFAULT_REF) {
            return true;
        }

        return locationDef.isPreferWrapper() && hasWrapper(project);
    }

    private static boolean hasWrapper(NbGradleProject project) {
        Path rootDir = DefaultGradleModelLoader.getAppliedRootProjectDir(project);
        Path wrapperPropertiesFile = rootDir
                .resolve("gradle")
                .resolve("wrapper")
                .resolve("gradle-wrapper.properties");
        return Files.isRegularFile(wrapperPropertiesFile);
    }


    public GradleConnector getGradleConnector() {
        return gradleConnector;
    }
}
