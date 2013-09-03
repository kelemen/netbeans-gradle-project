package org.netbeans.gradle.model.java;

/**
 * Defines the possible kinds of {@link JavaSourceGroup} instances.
 */
public enum JavaSourceGroupName {
    /** Defines a group for Java source files. */
    JAVA,

    /** Defines a group for Groovy source files. */
    GROOVY,

    /** Defines a group for Scala source files. */
    SCALA,

    /** Defines a group for resource files. */
    RESOURCES,

    /** Defines a group for other source files not classifiable into one of the other categories. */
    OTHER;
}
