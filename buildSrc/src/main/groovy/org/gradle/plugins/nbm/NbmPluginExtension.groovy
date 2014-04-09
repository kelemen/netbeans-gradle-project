package org.gradle.plugins.nbm

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 *
 * @author radim
 */
class NbmPluginExtension {
    String moduleName
    String specificationVersion
    boolean eager
    boolean autoload
    File keystore
    String nbm_alias
    String storepass

    private Configuration harnessConfiguration

    NbmPluginExtension(Project project) {
        harnessConfiguration = project.configurations.detachedConfiguration(
                project.dependencies.create('org.codehaus.mojo:nbm-maven-harness:7.4'))
    }

    Configuration getHarnessConfiguration() {
        harnessConfiguration
    }
}

