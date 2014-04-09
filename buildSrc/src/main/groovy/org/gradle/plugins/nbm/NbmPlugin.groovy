package org.gradle.plugins.nbm

import org.gradle.api.Action
import org.gradle.api.GradleException;
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

import java.util.concurrent.Callable

public class NbmPlugin implements Plugin<Project> {
    public static final String PROVIDED_COMPILE_CONFIGURATION_NAME = "providedCompile";
    public static final String PROVIDED_RUNTIME_CONFIGURATION_NAME = "providedRuntime";

    private static final String NBM_TASK = 'nbm'
    private static final String NETBEANS_TASK = 'netbeans'
    private static final String MANIFEST_TASK = 'generateModuleManifest'

    void apply(Project project) {
        project.logger.info "Registering deferred NBM plugin configuration..."
        project.plugins.withType(JavaPlugin) { configure(project) }

        def convention = new NbmPluginConvention(project)
        project.convention.plugins.nbm = convention
        project.tasks.withType(NbmTask.class).all { NbmTask task ->
            task.conventionMapping.nbmBuildDir = { convention.nbmBuildDir }
        }
        project.tasks.withType(ModuleManifestTask.class).all { ModuleManifestTask task ->
            task.conventionMapping.generatedManifestFile = { convention.generatedManifestFile }
        }
        project.tasks.withType(NetBeansTask.class).all { NetBeansTask task ->
            task.conventionMapping.moduleBuildDir = { convention.moduleBuildDir }
        }
        project.getTasks().withType(NetBeansTask.class, new Action<NetBeansTask>() {
            public void execute(NetBeansTask task) {
                task.dependsOn(new Callable() {
                    public Object call() throws Exception {
                        return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(
                                SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath();
                    }
                });
                task.classpath({
                        FileCollection runtimeClasspath = project.getConvention().getPlugin(JavaPluginConvention.class)
                                .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath();
                        Configuration providedRuntime = project.getConfigurations().getByName(
                                PROVIDED_RUNTIME_CONFIGURATION_NAME);
                        return runtimeClasspath.minus(providedRuntime);
                    });
            }
        });

        ModuleManifestTask manifestTask = project.tasks.create(MANIFEST_TASK, ModuleManifestTask)
        def userManifest = project.file('src' + File.separator + 'main' + File.separator + 'nbm' + File.separator + 'manifest.mf')
        if (userManifest.exists()) {
            project.tasks.jar.manifest.from { userManifest }
        }
        project.tasks.jar.manifest.from { manifestTask.getGeneratedManifestFile() }
        project.tasks.jar.dependsOn(manifestTask)

        NetBeansTask netbeansTask = project.tasks.create(NETBEANS_TASK, NetBeansTask)
        netbeansTask.dependsOn(project.tasks.jar)
        netbeansTask.setDescription("Generates a NetBeans module directory.");
        netbeansTask.setGroup(BasePlugin.BUILD_GROUP);
        // configure NBM task
        NbmTask nbmTask = project.tasks.create(NBM_TASK, NbmTask)
        nbmTask.dependsOn(netbeansTask)
        nbmTask.setGroup(BasePlugin.BUILD_GROUP)
        project.tasks.build.dependsOn(nbmTask)

        configureConfigurations(project.configurations)
    }

    public void configureConfigurations(ConfigurationContainer configurationContainer) {
        Configuration provideCompileConfiguration = configurationContainer.create(PROVIDED_COMPILE_CONFIGURATION_NAME).setVisible(false).
                setDescription("Additional compile classpath for libraries that should not be part of the NBM archive.");
        Configuration provideRuntimeConfiguration = configurationContainer.create(PROVIDED_RUNTIME_CONFIGURATION_NAME).setVisible(false).
                extendsFrom(provideCompileConfiguration).
                setDescription("Additional runtime classpath for libraries that should not be part of the NBM archive.");
        configurationContainer.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(provideCompileConfiguration);
        configurationContainer.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME).extendsFrom(provideRuntimeConfiguration);
    }

    private configure(Project project) {
        project.logger.info "Configuring NBM plugin..."

        project.extensions.nbm = new NbmPluginExtension(project)
        setupPropertiesMerging(project)
    }

    private void setupPropertiesMerging(Project project) {
        def mergeTask = project.tasks.add('mergeProperties', MergePropertiesTask)
        project.tasks.findByName('jar').dependsOn(mergeTask)
        def generatedClasses = "${project.buildDir}/generated-resources/main"
        def generatedResources = "${project.buildDir}/generated-resources/resources"
        def generatedOutput = "${project.buildDir}/generated-resources/output"

        project.sourceSets.main.output.dir(generatedOutput, builtBy: 'mergeProperties')
        def compileJavaTask = project.tasks.getByName('compileJava')
        compileJavaTask.outputs.dir(generatedClasses)
        compileJavaTask.doLast { JavaCompile it ->
            new File(generatedClasses).mkdirs()
            project.copy {
                from project.sourceSets.main.output.classesDir
                into generatedClasses
                include '**/*.properties'
                includeEmptyDirs false
            }
            project.fileTree(dir: project.sourceSets.main.output.classesDir).include('**/*.properties').visit {
                if (!it.isDirectory()) {
                    it.file.delete()
                }
            }

        }
        Copy processResourcesTask = project.tasks.getByName('processResources')
        processResourcesTask.outputs.dir(generatedResources)
        processResourcesTask.doLast { Copy it ->
            new File(generatedResources).mkdirs()
            project.copy {
                from project.sourceSets.main.output.resourcesDir
                into generatedResources
                include '**/*.properties'
                includeEmptyDirs false
            }
            project.fileTree(dir: project.sourceSets.main.output.resourcesDir).include('**/*.properties').visit {
                if (!it.isDirectory()) {
                    it.file.delete()
                }
            }

        }
    }
}
