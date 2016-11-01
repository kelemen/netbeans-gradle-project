package org.netbeans.gradle.build

import org.gradle.api.*
import org.gradle.api.tasks.bundling.*

class NbGradleMavenPublishPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.apply 'plugin': 'maven'

        project.configure(project.install.repositories.mavenInstaller) {
            pom.project {
                groupId = project.group;
                artifactId = project.name;
                version = project.version;
            }
        }

        project.uploadArchives {
            repositories {
                mavenDeployer {
                    def repoUrl = PropertyUtils.getStringProperty(project, 'publishNBGradleRepoUrl', 'https://api.bintray.com/maven/kelemen/maven/Gradle-NetBeans');
                    def repoUser = PropertyUtils.getStringProperty(project, 'publishNBGradleUserName', 'kelemen');
                    def repoPassword = PropertyUtils.getStringProperty(project, 'publishNBGradlePassword', '');
                    repository(url: repoUrl) {
                        authentication(userName: repoUser, password: repoPassword);
                    }
                }
            }
        }
    }
}
