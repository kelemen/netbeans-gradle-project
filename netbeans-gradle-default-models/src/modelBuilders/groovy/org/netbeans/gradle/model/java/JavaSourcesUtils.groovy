package org.netbeans.gradle.model.java

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet

class JavaSourcesUtils {
    static SourceDirectorySet getGroovySources(SourceSet sourceSet) {
        return sourceSet.groovy
    }

    static SourceDirectorySet getScalaSources(SourceSet sourceSet) {
        return sourceSet.scala
    }

    private JavaSourcesUtils() {
        throw new AssertionError()
    }
}

