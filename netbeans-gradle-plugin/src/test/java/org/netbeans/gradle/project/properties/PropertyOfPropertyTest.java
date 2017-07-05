package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PropertyOfPropertyTest {
    @Test
    public void testSubValueChanges() {
        TestProperty property = new TestProperty();
        PropertySource<Integer> propertyOfProperty = property.getPropertyOfProperty();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

        property.assertValue(0);

        verifyZeroInteractions(listener);

        property.setSubValue(1);
        property.assertValue(1);
        verify(listener, only()).run();

        listenerRef.unregister();

        property.setSubValue(2);
        property.assertValue(2);

        verify(listener, only()).run();
    }

    @Test
    public void testSubPropertyChanges() {
        TestProperty property = new TestProperty();
        PropertySource<Integer> propertyOfProperty = property.getPropertyOfProperty();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

        verifyZeroInteractions(listener);

        property.setSubProperty(1);
        property.assertValue(1);
        verify(listener, only()).run();

        listenerRef.unregister();

        property.setSubProperty(2);
        property.assertValue(2);

        verify(listener, only()).run();
    }

    @Test
    public void testSubPropertyChangesPreviousUntracked() {
        TestProperty property = new TestProperty();
        PropertySource<Integer> propertyOfProperty = property.getPropertyOfProperty();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

        verifyZeroInteractions(listener);

        MutableProperty<Integer> prevSubProperty = property.setSubProperty(1);
        verify(listener, only()).run();

        prevSubProperty.setValue(5);
        property.assertValue(1);
        verify(listener, only()).run();

        listenerRef.unregister();
    }

    @Test
    public void testSubPropertyAndSubValueChanges() {
        TestProperty property = new TestProperty();
        PropertySource<Integer> propertyOfProperty = property.getPropertyOfProperty();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

        verifyZeroInteractions(listener);

        property.setSubProperty(1);
        property.assertValue(1);
        verify(listener, only()).run();

        property.setSubValue(2);
        property.assertValue(2);
        verify(listener, times(2)).run();

        listenerRef.unregister();

        property.setSubValue(3);
        property.assertValue(3);

        property.setSubProperty(4);
        property.assertValue(4);

        verify(listener, times(2)).run();
    }

    private static final class TestProperty {
        private final MutableProperty<MutableProperty<Integer>> property;
        private final PropertyOfProperty<MutableProperty<Integer>, Integer> wrapper;

        public TestProperty() {
            this.property = PropertyFactory.memProperty(PropertyFactory.memProperty(0));
            this.wrapper = new PropertyOfProperty<>(property, arg -> arg);
        }

        public MutableProperty<Integer> setSubProperty(int value) {
            MutableProperty<Integer> prevSubProperty = property.getValue();
            property.setValue(PropertyFactory.memProperty(value));
            return prevSubProperty;
        }

        public void setSubValue(int value) {
            property.getValue().setValue(value);
        }

        public PropertySource<Integer> getPropertyOfProperty() {
            return wrapper;
        }

        public void assertValue(int expectedValue) {
            int value = wrapper.getValue();
            assertEquals("value", expectedValue, value);
        }
    }
}
