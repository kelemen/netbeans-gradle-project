package org.netbeans.gradle.model.java

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.netbeans.gradle.model.util.GradleVersionUtils

class JavaSourcesUtils {
    static SourceDirectorySet getGroovySources(SourceSet sourceSet) {
        return sourceSet.groovy
    }

    static SourceDirectorySet getScalaSources(SourceSet sourceSet) {
        return sourceSet.scala
    }

    static Set<File> getClassesDirs(SourceSetOutput outputDir) {
        if (GradleVersionUtils.GRADLE_4_OR_BETTER) {
            return outputDir.getClassesDirs().getFiles()
        } else {
            return Collections.singleton(outputDir.getClassesDir())
        }
    }

    private JavaSourcesUtils() {
        throw new AssertionError()
    }
}

