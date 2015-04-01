package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.util.NbFunction;

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

    private static final class TestProperty {
        private final MutableProperty<MutableProperty<Integer>> property;
        private final PropertyOfProperty<MutableProperty<Integer>, Integer> wrapper;

        public TestProperty() {
            this.property = PropertyFactory.memProperty(PropertyFactory.memProperty(0));
            this.wrapper = new PropertyOfProperty<>(property, new NbFunction<MutableProperty<Integer>, PropertySource<Integer>>() {
                @Override
                public PropertySource<Integer> apply(MutableProperty<Integer> arg) {
                    return arg;
                }
            });
        }

        public void setSubProperty(int value) {
            property.setValue(PropertyFactory.memProperty(value));
        }

        public void setSubValue(int value) {
            property.getValue().setValue(value);
        }

        public PropertySource<Integer> getPropertyOfProperty() {
            return wrapper;
        }

        public void assertValue(int expectedValue) {
            assertEquals("value", expectedValue, wrapper.getValue().intValue());
        }
    }
}
