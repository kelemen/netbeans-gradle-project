Downloads
---------

The latest build (nbm file) can be downloaded from
[http://plugins.netbeans.org/plugin/44510/gradle-support](http://plugins.netbeans.org/plugin/44510/gradle-support).

General Description
-------------------

This project is a NetBeans plugin able to open Gradle based Java projects.
The implementation is based on [Geertjan Wielenga's](https://blogs.oracle.com/geertjan/) plugin.
You can open a folder as a project in NetBeans like with any other project and
start editing the code without actually generating any other NetBeans project files.

The project is usable with no major limitations. Open a new issue if you believe something needs
to be fixed.

You can read more about the plugin on the [wiki pages](https://github.com/kelemen/netbeans-gradle-project/wiki).

Troubleshooting
--------------

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
- It is not detected automtically when the project needs to be reloaded.
  The "Reload Project" action must be executed manually (from the project's
  popup menu). No automatic detection is done because reloading the project is
  slow (just like opening it).
- In case a directory for a source set does not exists it will be unavailable
  in the IDE and you cannot add files to it from the IDE.
- Exclusion of directories from source sets are ignored. I don't see how it
  is possible in NetBeans (efficiently) because you cannot create your own
  ClassPath implementation.
- Exclusion of dependencies are ignored. This is probably not too much of an issue,
  since they will not be ignored when executing gradle tasks and they should not be
  relevant when editing the code.
- Every dependency is assumed to be a transitive dependency. This is mainly
  a performance issue (but not much since loading the models by the
  tooling API takes a lot more time) because tasks are executed by Gradle itself
  which of course considers these.
- Resource directories are detected based on their name because I could not
  find any reliable way to do it with the tooling API. So every source
  directory whose last directory in its path starts with "resource"
  (not case-sensitive) is considered a resource directory.
- The *sourceCompatibility* and the *targetCompatibility* in the build script are ignored
  but these can be set separately in project properties (stored with the
  *settings.gradle* in *.nb-gradle-properties*).
