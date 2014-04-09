package org.gradle.plugins.nbm

import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class NbmTask extends ConventionTask {

    @OutputFile
    File getOutputFile() {
        def moduleJarName = netbeansExt().moduleName.replace('.', '-')
        new File(getNbmBuildDir(), moduleJarName + '.nbm')
    }

    @OutputDirectory
    File nbmBuildDir

    @InputFiles
    FileCollection getModuleFiles() {
        project.files(project.tasks.netbeans.getModuleBuildDir()).builtBy(project.tasks.netbeans)
    }

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    @TaskAction
    void generate() {
        project.logger.info "NbmTask running"
        def nbmFile = getOutputFile()
        def nbmDir = getNbmBuildDir()
        if (!nbmDir.isDirectory()) {
            nbmDir.mkdirs()
        }

        def moduleJarName = netbeansExt().moduleName.replace('.', '-')

        def makenbm = antBuilder().antProject.createTask("makenbm")
        makenbm.productDir = project.tasks.netbeans.getModuleBuildDir()
        makenbm.file = nbmFile
        makenbm.module = "modules" + File.separator + moduleJarName + ".jar"

        if (netbeansExt().keystore) {
            def signature = makenbm.createSignature()
            signature.keystore = netbeansExt().keystore
            signature.alias = netbeansExt().nbm_alias
            signature.storepass = netbeansExt().nbm_alias
        }
        makenbm.execute()
    }

    private AntBuilder antBuilder() {
        def antProject = ant.antProject
        ant.project.getBuildListeners().firstElement().setMessageOutputLevel(3)
        Taskdef taskdef = antProject.createTask("taskdef")
        taskdef.classname = "org.netbeans.nbbuild.MakeNBM"
        taskdef.name = "makenbm"
        taskdef.classpath = new Path(antProject, netbeansExt().harnessConfiguration.asPath)
        taskdef.execute()
        return getAnt();
    }
}

