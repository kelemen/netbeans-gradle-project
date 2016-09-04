package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.property.GradleProperty;

public final class ProjectPropertiesApi {
    public static GradleProperty.SourceEncoding sourceEncoding(final PropertySource<Charset> property) {
        ExceptionHelper.checkNotNullArgument(property, "property");
        return new GradleProperty.SourceEncoding() {
            @Override
            public Charset getValue() {
                return property.getValue();
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return NbListenerRefs.asNbRef(property.addChangeListener(listener));
            }
        };
    }

    public static GradleProperty.ScriptPlatform scriptPlatform(final PropertySource<ScriptPlatform> property) {
        ExceptionHelper.checkNotNullArgument(property, "property");
        return new GradleProperty.ScriptPlatform() {
            @Override
            public JavaPlatform getValue() {
                ScriptPlatform result = property.getValue();
                return result != null ? result.getJavaPlatform() : null;
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return NbListenerRefs.asNbRef(property.addChangeListener(listener));
            }
        };
    }

    public static GradleProperty.SourceLevel sourceLevel(final PropertySource<String> property) {
        ExceptionHelper.checkNotNullArgument(property, "property");
        return new GradleProperty.SourceLevel() {
            @Override
            public String getValue() {
                return property.getValue();
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return NbListenerRefs.asNbRef(property.addChangeListener(listener));
            }
        };
    }

    public static GradleProperty.BuildPlatform buildPlatform(final PropertySource<ProjectPlatform> property) {
        ExceptionHelper.checkNotNullArgument(property, "property");
        return new GradleProperty.BuildPlatform() {
            @Override
            public ProjectPlatform getValue() {
                return property.getValue();
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return NbListenerRefs.asNbRef(property.addChangeListener(listener));
            }
        };
    }

    private ProjectPropertiesApi() {
        throw new AssertionError();
    }
}
