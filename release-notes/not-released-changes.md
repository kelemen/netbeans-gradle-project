- It is now possible to configure a shortcut for the reload project action (Name = "Reload Gradle Project", Category = "Project").


### For NetBeans 9 only:

- Changed license to Apache 2.0
- Removed depracted API and various backward incompatible changes.
- Upgraded Gradle API (and therefore the default Gradle version) to 4.0.1

### Internal development changes

- Instead of the *jdkXXXCompiler* property, the build now needs a *jdkXXXHome* property.
- Fixes a problem with composite builds where composite builds were not always properly recognized.
