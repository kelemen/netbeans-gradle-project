package org.gradle.plugins.nbm

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

class MergePropertiesTask extends ConventionTask {

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    MergePropertiesTask() {
        configure { ConventionTask it ->
            def generatedClasses = "${project.buildDir}/generated-resources/main"
            def generatedResources = "${project.buildDir}/generated-resources/resources"
            // def generatedOutput = "${project.buildDir}/generated-resources/output"
            outputs.dir(new File(project.buildDir, '/generated-resources/output'))
            inputs.dir generatedClasses
            inputs.dir generatedResources
            dependsOn project.tasks.findByName('compileJava')
            dependsOn project.tasks.findByName('processResources')
        }
    }

    @TaskAction
    void generate() {
        def generatedClasses = "${project.buildDir}/generated-resources/main"
        def generatedResources = "${project.buildDir}/generated-resources/resources"
        def generatedOutput = "${project.buildDir}/generated-resources/output"
        new File(generatedOutput).mkdirs()

        Set<String> paths = new HashSet<>()
        def genProperties = project.fileTree(dir: generatedClasses)
        def userProperties = project.fileTree(dir: generatedResources)

        genProperties.visit { if (!it.file.isDirectory()) paths.add(it.relativePath.pathString) }
        userProperties.visit { if (!it.file.isDirectory()) paths.add(it.relativePath.pathString) }
        paths.each { String path ->
            // if in both merge else copy
            def dest = new File(generatedOutput, path).parentFile
            dest.mkdirs()
            if (!new File(generatedClasses, path).exists()) {
                project.copy {
                    from new File(generatedResources, path)
                    into dest
                }
            } else if (!new File(generatedResources, path).exists()) {
                project.copy {
                    from new File(generatedClasses, path)
                    into dest
                }
            } else {
                def destFile = new File(generatedOutput, path)
                destFile << new File(generatedClasses, path).text +
                        '\n' +
                        new File(generatedResources, path).text
            }
        }
    }
}
