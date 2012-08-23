General Description
-------------------

This project is a NetBeans plugin able to open Gradle based Java projects.
The implementation is based on [Geertjan Wielenga's](https://blogs.oracle.com/geertjan/) plugin.
You can open a folder as a project in NetBeans like with any other project and
start editing the code without actually generating any other NetBeans project files.

The project is usable with no major limitations. Open a new issue if you believe something needs
to be fixed.

Troubeshooting
--------------

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

### Every time a Gradle task fails NetBeans shows an error message ###

I know but it seems Gradle itself needs to modified to prevent these messages.

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
- Errors occurring while loading the project is not displayed to the user but
  only emitted as *WARNING* level logs.
- Debugging is only possible by manually attaching to the process. To run a project
  in debug mode a task must be defined to start the project in debug mode and listen
  on a particular port. Single test run can be started by right clicking the file in
  the project view but attaching to the process must still be done manually.
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
- Only a single task can be executed (and the built-in tasks, like rebuild). Custom
  tasks will be needed to make the project really usable. Also global custom tasks
  like with the Maven plugin would be nice as well. Allowing to execute custom tasks
  is relatively straightforward to implement.
