- Recognizes "error" and "warning" lines with file paths in the output window as links.
- Show the version number of Gradle as the plugin sees it in JDK selection combo boxes (rather than just the display name).
- Print the version of Gradle used to execute a task (opt-in: must be configured in the global settings).
- Fixed some Gradle API usage deprecated for Gradle 5.0

### For NetBeans 9 only:

- Basic JDK 9 support (there are still some issues with test sources).


### Internal development changes

- Instead of the *jdkXXXCompiler* property, the build now needs a *jdkXXXHome* property.
