Current changes
---------------

The release notes for all versions (since 1.1.1) can now be viewed in the *release-notes* directory.
Also, there is a list of changes applied but not yet released in
[not-released-changes.md](https://github.com/kelemen/netbeans-gradle-project/blob/master/release-notes/not-released-changes.md).


Downloads
---------

The latest release can be downloaded from
[http://plugins.netbeans.org/plugin/44510/gradle-support](http://plugins.netbeans.org/plugin/44510/gradle-support) and is also
available via the Update Center (Tools/Plugins: Look for "Gradle Support").

**JDK 6**: If you are using NetBeans with JDK 6, the last version of the plugin compatible with Java 6 is [1.3.0](http://dl.bintray.com/kelemen/maven/com/github/kelemen/netbeans-gradle-plugin/1.3.0/netbeans-gradle-plugin-1.3.0.nbm). Note: You will still be able to open Java 6 based projects (even Java 5). The limitation applies only for the JDK used to start NetBeans.

General Description
-------------------

This project is a NetBeans plugin able to open Gradle based Java projects.
The implementation is based on [Geertjan Wielenga's](https://blogs.oracle.com/geertjan/) plugin.
You can open a folder as a project in NetBeans like with any other project and
start editing the code without actually generating any other NetBeans project files.

**About J2EE projects**: This plugin sees web projects as simple J2SE projects with an additional "webapp" directory, so many
features related to web projects will be disabled in Gradle projects. Hildo's
[Gradle JavaEE Support](http://plugins.netbeans.org/plugin/55529/gradle-javaee-support) might be installed for better support.

You can read more about the plugin on the [wiki pages](https://github.com/kelemen/netbeans-gradle-project/wiki).

Troubleshooting
--------------

### I have updated the plugin and now cannot load projects ###

You may see an exception like this: `FileNotFoundException: JAR entry XXX.class not found in YYY\gradle-tooling-api.jar`.
This happens because the Gradle daemon caches some jars and some of them might change when updating. The issue can be
reproduced using Gradle 1.8 and above when using the new API to load a project (can be adjusted in the global settings).

To fix this, you have to kill the Gradle daemon (a java process) and reload the project.

### LinkageError: "loader constraint violation", debugging does not work ###

Attempting to use Gradle 1.8 will break debugging functionality in NetBeans (not only for Gradle projects). The workaround is not to use Gradle 1.8: Specify another version to load the Gradle project in the project properties. Note that once you have loaded a Gradle 1.8 project, you have to restart NetBeans to fix debugging features.

The issue in Gradle is fixed in *Gradle 1.9-rc-1*. However, when using nightly builds, you have to use *1.10-20131011231056+0000* or later.

Read issue [#74](https://github.com/kelemen/netbeans-gradle-project/issues/74) for details.

### NetBeans hangs while starting ###

In version 1.1.7 there is a known issue which might cause a dead-lock in NetBeans.
It is fixed in 1.1.8 but to be able to upgrade you will need to
[disable the currently installed one manually](https://blogs.oracle.com/gridbag/entry/disabling_a_netbeans_plugin_before).

### The classpaths are wrong within NetBeans ###

This plugin loads an [IdeaProject](http://gradle.org/docs/current/groovydoc/org/gradle/plugins/ide/idea/model/IdeaProject.html)
instance from the build script and therefore configurations applied to the
[Idea plugin](http://gradle.org/docs/current/userguide/idea_plugin.html) (`apply plugin: 'idea'`)
will be detected by the NetBeans plugin as well.

Therefore you can add code to the build script to manually edit the classpath:

    idea {
      module {
        scopes.COMPILE.plus += configurations.myconfig;
      }
    }

The above code adds the dependencies of the "myconfig" configuration to the compile time classpath
of the subproject. Don't forget to call `apply plugin: 'idea'` before the above code can be applied.

### How can I configure the provided dependency scope? ###

Gradle does not yet have a concept of provided dependencies in the Java plugin (it does have
in the [War plugin](http://gradle.org/docs/current/userguide/war_plugin.html)).
Most developers solve this by adding a user defined configuration like this:

    configurations {
      provided
    }

Then after adding the dependencies, adjusts the classpaths:

    sourceSets {
      main {
        compileClasspath = compileClasspath + configurations.provided;
      }
    }

This way Gradle will be able to compile the sources. However, the NetBeans plugin
is ignorant of user defined configurations and therefore to make the project load
properly in NetBeans, you will need to append the following code to the build script:

    idea {
      module {
        scopes.PROVIDED.plus += configurations.provided;
      }
    }

In version "1.1.3" or below, the above code should be

    idea {
      module {
        scopes.COMPILE.plus += configurations.provided;
      }
    }

because the NetBeans plugin did not expect provided dependencies.
Note however that in both cases provided dependencies will be listed
amongst the compile time dependencies within NetBeans.

### NetBeans seems to load projects without asking ###

Most likely you have a project with more than 100 subprojects (including the root).
To avoid unnecessary loading of projects you should increase the number of projects
to be cached. This can be changed on the *Tools/Options/Miscellaneous/Gradle* panel.

### Loading the project takes too long ###

There can be several causes for this problem:

- On the first project load, Gradle will download every
  dependency and this may take quite a lot of time if you have lots
  of dependencies.
- If loading the project waits on "Load projects" then chances are that
  the gradle daemon got corrupted. You should delete the "daemon" directory
  in the ".gradle" directory of your home directory (System.getProperty("user.home")).
  If it does not help, remove the entire ".gradle" directory (note that this will also
  remove your local artifact cache).
- If loading the project waits while trying to retrieving the dependency of a project,
  you might need to configure the proxy settings in NetBeans.

### Cannot load a project: The error message says that the Gradle daemon gave up after 100 attempts ###

The Gradle daemon got corrupted. You should delete the "daemon" directory in the ".gradle"
directory of your home directory (System.getProperty("user.home")). If it does not help,
remove the entire ".gradle" directory (note that this will also remove your local
artifact cache).

### How can I speed up the project opening? ###

The only thing you can do is to specify the installation directory of Gradle.
This can be done on the *Tools/Options/Miscellaneous/Gradle* panel. This way
you can even jump to the Gradle source code from a Gradle script (using ctrl + click).

### I do not have a *build.gradle* file for every project. How can I open these projects? ###

Open the parent project of the project you want to open. Once you have opened the parent project,
you may right click on any of its child projects and open it even if it does not have
a *build.gradle*.

Note however, that I recommend to have a *build.gradle* file for every project
(even if empty) you might want to open because even if you open projects without
a *build.gradle*, these opened projects will not be opened the next time you
start NetBeans.

### I have a *build.gradle* file in a directory but NetBeans does not recognizes it as a project ###

Gradle projects found in the temporary directory cannot be opened for techincal reasons.
Move these projects from the temporary directory.

### Classes of the Gradle API are not recognized by the editor ###

I know of this problem but it seems that it is not possible (with reasonable effort)
to solve this without a new NetBeans API. Don't despair, there is a hope that this change
might get added (note however that this may not be trivial to add to NetBeans).

### I get the error: "Cannot rename file *settings.gradle* in XXXX to *build.gradle*" ###

You might have an old Gradle plugin installed and it seems that they cannot co-exists.
Remove the old plugin. See [Issue #4](https://github.com/kelemen/netbeans-gradle-project/issues/4)
for further information.

### Gradle files get renamed to *build.gradle* ###

The same as the previous issue:

You might have an old Gradle plugin installed and it seems that they cannot co-exists.
Remove the old plugin. See [Issue #4](https://github.com/kelemen/netbeans-gradle-project/issues/4)
for further information.

Current Limitations
-------------------

- It is not possible to directly open projects without a *build.gradle*.
  To open such projects, open its parent project and find the project to
  be opened in the *SubProjects* node, then right click/"Open Subproject".
- The character encoding for the source file is not read from the Gradle
  script but can be set separately in project properties (stored with the
  *settings.gradle* in *.nb-gradle-properties*).
- It is not detected automatically when the project needs to be reloaded.
  The "Reload Project" action must be executed manually (from the project's
  popup menu). No automatic detection is done because reloading the project is
  slow (just like opening it).
