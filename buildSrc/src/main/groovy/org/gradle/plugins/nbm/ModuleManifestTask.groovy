package org.gradle.plugins.nbm

import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

class ModuleManifestTask extends ConventionTask {
    @OutputFile
    File generatedManifestFile

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    @TaskAction
    void generate() {
        def manifestFile = getGeneratedManifestFile()
        project.logger.info "Generating NetBeans module manifest $generatedManifestFile"

        Map<String, String> moduleDeps = new HashMap<>()
        def mainSourceSet = project.sourceSets.main
        def compileConfig = project.configurations.findByName(mainSourceSet.compileConfigurationName)
        def resolvedConfiguration = compileConfig.resolvedConfiguration
        resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency it ->
            // println 'module ' + it.name + ', ' + it.id.id
            it.moduleArtifacts.each { a ->
                // println '  artifact ' + a + ' file ' + a.file
                if (a.file?.exists() && 'jar' == a.extension) {
                    JarFile jar = new JarFile(a.file)
                    def attrs = jar.manifest?.mainAttributes
                    def moduleName = attrs?.getValue(new Attributes.Name('OpenIDE-Module'))
                    def moduleVersion = attrs?.getValue(new Attributes.Name('OpenIDE-Module-Specification-Version'))
                    if (moduleName && moduleVersion) {
                        moduleDeps.put(moduleName, moduleVersion)
                    }
                }
            }
        }

        def manifest = new Manifest()
        def mainAttributes = manifest.getMainAttributes()
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, '1.0')
        mainAttributes.put(new Attributes.Name('OpenIDE-Module'), netbeansExt().moduleName)
        if (netbeansExt().specificationVersion) {
            mainAttributes.put(new Attributes.Name('OpenIDE-Module-Specification-Version'), netbeansExt().specificationVersion)
        }
        if (!moduleDeps.isEmpty()) {
            mainAttributes.put(
                    new Attributes.Name('OpenIDE-Module-Module-Dependencies'),
                    moduleDeps.entrySet().collect { it.key + ' > ' + it.value }.join(', '))
        }
        def classpath = computeClasspath()
        if (classpath != null && !classpath.isEmpty()) {
            mainAttributes.put(new Attributes.Name('Class-Path'), classpath)
        }

        def requires = netbeansExt().requires;
        if (!requires.isEmpty()) {
            mainAttributes.put(new Attributes.Name('OpenIDE-Module-Requires'), requires.join(', '))
        }
        def os = new FileOutputStream(manifestFile)
        manifest.write(os)
        os.close()
    }

    private String computeClasspath() {
        FileCollection classpath = project.tasks.findByPath('netbeans').classpath
        def jarNames = []
        classpath.asFileTree.visit { FileVisitDetails fvd ->
            if (fvd.directory) return
            if (!fvd.name.endsWith('jar')) return

            JarFile jar = new JarFile(fvd.file)
            def attrs = jar.manifest.mainAttributes
            def attrValue = attrs.getValue(new Attributes.Name('OpenIDE-Module'))
            if (attrValue != null) return

            // JAR but not NetBeans module
            jarNames += 'ext/' + fvd.name
        }
        jarNames.join(' ')
    }
}
