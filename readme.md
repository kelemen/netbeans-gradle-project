General Description
===================

This project is a NetBeans plugin able to open Gradle based Java projects.
The implementation is based on [Geertjan Wielenga's](https://blogs.oracle.com/geertjan/) plugin.
You can open a folder as a project in NetBeans like with any other project and
start editing the code without actually generating any other NetBeans project files.

In its current state this project is roughly usable but is far from being done.

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
- In its current state this plugin does not add any support for unit tests,
  although new classes can be added manually (and can also be edited). The
  class path for unit tests are set appropriatelly (hopefully) however.
- Every directory for every sourceset will automatically be created. Note that
  the *java* plugin of Gradle implictly defines four source sets
  (namely: compile, resources, test compile and test resources). The directories
  are created so that you can add new files to every source set conveniently.
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
- There is no support for debugging. You can define a task which starts the
  application in debug and then attach to the process but NetBeans will not be
  able to find the sources (so debugging is limited). Finding the sources
  can be remedied probably without too much effort.
- Resource directories are detected based on their name because I could not
  find any reliable way to do it with the tooling API. So every source
  directory whose last directory in its path starts with "resource"
  (not case-sensitive) is considered a resource directory.
- The default JDK will be used for the project. I could not find a good way
  to retrieve the *targetCompatibility* attribute with the tooling API.
- Only a single task can be executed. Custom tasks will be needed to make
  the project really usable. Also global custom tasks like with the Maven
  plugin would be nice as well. Allowing to execute custom tasks is relatively
  straightforward to implement.
- Sources for dependencies cannot be downloaded nor attached. This could only
  be implemented like in the Maven plugin if the tooling API is updated.
- This might be not a limitation for the user but the code base needs a major
  cleanup.
