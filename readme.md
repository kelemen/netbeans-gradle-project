General Description
===================

This project is a NetBeans plugin able to open Gradle based Java projects.
The implementation is based on [Geertjan Wielenga's](https://blogs.oracle.com/geertjan/) plugin.
You can open a folder as a project in NetBeans like with any other project and
start editing the code without actually generating any other NetBeans project files.

The project is actually usable, with the following major limitations:

- The only way to reload the project is to restart NetBeans. Currently this plugin
  makes no attempt to automatically reload the project. Reloading the project is
  actually necessary if the source sets or the dependencies change.
- Only the default JDK can be used for source and targe compatibility.


Current Limitations
===================

- It takes some time before you can actually start edit the code
  because the Gradle Tooling API will do everything before actually
  returning models to work on. This is especially painful on the first
  project open because it will actually download every single dependency
  before returning (even test dependencies).
- NetBeans needs to be restarted if *build.gradle* or *settings.gradle* changes
  (and also if something changes which changes the result of build.gradle).
  To change this the code needs some refactorings to be able to safely add
  these features. Note that it is impossible to detect every changes when
  the project need to be reloaded, so a *Reload* button will also be needed.
- The code does not check for any errors while loading the the project, so
  errors in *build.gradle* (etc.) will cause NetBeans to pop-up a dialog
  reporting an unexpected exception (which you should not report to
  NetBeans developers of course).
- It is not possible to specify the Gradle installation directory, only the
  bundled Gradle version can be used (i.e.: 1.0). I don't know how to do this
  because I'm not familiar with class loading mechanism of NetBeans.
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
- The default JDK will be used for the project. I could not find a good way
  to retrieve the *sourceCompatibility* and the *targetCompatibility* attribute
  with the tooling API.
- Only a single task can be executed (and the built-in tasks, like rebuild). Custom
  tasks will be needed to make the project really usable. Also global custom tasks
  like with the Maven plugin would be nice as well. Allowing to execute custom tasks
  is relatively straightforward to implement.
- Sources for dependencies cannot be downloaded nor attached. This could only
  be implemented like in the Maven plugin if the tooling API is updated.
- This might be not a limitation for the user but the code base needs a major
  cleanup.
