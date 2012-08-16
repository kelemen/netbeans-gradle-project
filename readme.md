General Description
===================

This project is a NetBeans plugin able to open Gradle based Java projects.
The implementation is based on [Geertjan Wielenga's](https://blogs.oracle.com/geertjan/) plugin.
You can open a folder as a project in NetBeans like with any other project and
start editing the code without actually generating any other NetBeans project files.

The project is usable with no major limitations. Open a new issue if you believe something needs
to be fixed.


Current Limitations
===================

- It takes some time before you can actually start edit the code
  because the Gradle Tooling API will do everything before actually
  returning models to work on. This is especially painful on the first
  project open because it will actually download every single dependency
  before returning (even test dependencies).
- The character encoding for the source file is not read from the Gradle
  script but can be set separately in project properties (stored with the
  *settings.gradle* in *.nb-gradle-properties*).
- It is not detected automtically when the project needs to be reloaded.
  The "Reload Project" action must be executed manually (from the project's
  popup menu). No automatic detection is done because reloading the project is
  slow (just like opening it).
- Errors occurring while loading the project is not displayed to the user but
  only emitted as *WARNING* level logs.
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
- The *sourceCompatibility* and the *targetCompatibility* in the build script are ignored
  but these can be set separately in project properties (stored with the
  *settings.gradle* in *.nb-gradle-properties*).
- Only a single task can be executed (and the built-in tasks, like rebuild). Custom
  tasks will be needed to make the project really usable. Also global custom tasks
  like with the Maven plugin would be nice as well. Allowing to execute custom tasks
  is relatively straightforward to implement.
