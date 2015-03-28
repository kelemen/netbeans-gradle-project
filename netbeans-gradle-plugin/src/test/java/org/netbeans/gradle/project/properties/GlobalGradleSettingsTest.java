package org.netbeans.gradle.project.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jtrim.property.MutableProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;

import static org.junit.Assert.*;

public class GlobalGradleSettingsTest {
    private GlobalGradleSettings settings;

    @Before
    public void setUp() {
        settings = new GlobalGradleSettings("GlobalGradleSettingsTest");
    }

    @After
    public void tearDown() {
    }

    private static List<String> staticList(String... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements.clone()));
    }

    private static <T> void testGetAndSet(MutableProperty<T> property, T value) {
        testGetAndSet(property, value, value);
    }

    private static <T> void testGetAndSet(MutableProperty<T> property, T value, T expected) {
        property.setValue(value);
        assertEquals(expected, property.getValue());
    }

    @Test
    public void testJvmArgs_0Arg() {
        testGetAndSet(settings.gradleJvmArgs(), Collections.<String>emptyList(), null);
    }

    @Test
    public void testJvmArgs_1Arg() {
        testGetAndSet(settings.gradleJvmArgs(), staticList("arg1"));
    }

    @Test
    public void testJvmArgs_2Args() {
        testGetAndSet(settings.gradleJvmArgs(), staticList("arg1", "arg2"));
    }

    @Test
    public void testJvmArgs_ManyArgs() {
        testGetAndSet(settings.gradleJvmArgs(), staticList("arg1", "arg2", "arg3", "arg4", "arg5"));
    }

    @Test
    public void testJvmArgs_NonAsciiChar() {
        testGetAndSet(settings.gradleJvmArgs(), staticList("arg1\uA356\u0120sd"));
    }
}
