package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.property.GradleProperty;

public final class ProjectPropertiesApi {
    public static GradleProperty.SourceEncoding sourceEncoding(MutableProperty<Charset> property) {
        final NbPropertySourceWrapper<Charset> result = new NbPropertySourceWrapper<>(property);
        return new GradleProperty.SourceEncoding() {
            @Override
            public Charset getValue() {
                return result.getValue();
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return result.addChangeListener(listener);
            }
        };
    }

    public static GradleProperty.ScriptPlatform scriptPlatform(MutableProperty<JavaPlatform> property) {
        final NbPropertySourceWrapper<JavaPlatform> result = new NbPropertySourceWrapper<>(property);
        return new GradleProperty.ScriptPlatform() {
            @Override
            public JavaPlatform getValue() {
                return result.getValue();
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return result.addChangeListener(listener);
            }
        };
    }

    public static GradleProperty.SourceLevel sourceLevel(MutableProperty<String> property) {
        final NbPropertySourceWrapper<String> result = new NbPropertySourceWrapper<>(property);
        return new GradleProperty.SourceLevel() {
            @Override
            public String getValue() {
                return result.getValue();
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return result.addChangeListener(listener);
            }
        };
    }

    public static GradleProperty.BuildPlatform buildPlatform(MutableProperty<ProjectPlatform> property) {
        final NbPropertySourceWrapper<ProjectPlatform> result = new NbPropertySourceWrapper<>(property);
        return new GradleProperty.BuildPlatform() {
            @Override
            public ProjectPlatform getValue() {
                return result.getValue();
            }

            @Override
            public NbListenerRef addChangeListener(Runnable listener) {
                return result.addChangeListener(listener);
            }
        };
    }

    private ProjectPropertiesApi() {
        throw new AssertionError();
    }
}
