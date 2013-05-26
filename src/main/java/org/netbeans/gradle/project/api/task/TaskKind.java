package org.netbeans.gradle.project.api.task;

/**
 * Defines the possible kinds of built-in commands. The kind of action affects
 * the output window handling of the Gradle project. Commands of different kind
 * will not share the output window and so they may not overwrite each others
 * output.
 *
 * @see CustomCommandActions
 */
public enum TaskKind {
    /**
     * Defines the most common command type. It is recommended for build, clean,
     * test, etc.
     */
    BUILD,

    /**
     * Defines a command which compiles and executes the project. It is
     * recommended for commands: run and run file.
     */
    RUN,

    /**
     * Defines a command which will allow the user to debug a code. It is
     * recommended for all kinds of debug commands.
     */
    DEBUG,

    /**
     * Defines a command which will list the executed Gradle tasks in the
     * caption of the output window. Note that two different commands with
     * this type may use separate output window.
     * <P>
     * This type is not recommended for built-in commands because it might cause
     * may output windows to be opened which is not user friendly.
     */
    OTHER
}
