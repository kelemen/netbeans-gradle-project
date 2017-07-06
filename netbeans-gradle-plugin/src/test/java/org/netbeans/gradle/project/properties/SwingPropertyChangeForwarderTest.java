package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class SwingPropertyChangeForwarderTest {
    private static SwingPropertyChangeForwarder fromProperties(TestProperty... properties) {
        SwingPropertyChangeForwarder.Builder result = new SwingPropertyChangeForwarder.Builder();
        for (TestProperty property: properties) {
            property.addTo(result);
        }

        return result.create();
    }

    @Test
    public void testSingleProperty() {
        TestProperty property = new TestProperty("my-property");

        SwingPropertyChangeForwarder listenerForwarder = fromProperties(property);

        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        listenerForwarder.addPropertyChangeListener(listener);

        verifyZeroInteractions(listener);

        property.setValue(1);
        property.verifyMock(listener, 1);

        listenerForwarder.removePropertyChangeListener(listener);

        property.setValue(2);
        property.verifyMock(listener, 1);

        listenerForwarder.checkListenerConsistency();
    }

    @Test
    public void testTwoProperties() {
        TestProperty property1 = new TestProperty("my-property1");
        TestProperty property2 = new TestProperty("my-property2");

        SwingPropertyChangeForwarder listenerForwarder = fromProperties(property1, property2);

        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        listenerForwarder.addPropertyChangeListener(listener);

        verifyZeroInteractions(listener);

        property1.setValue(1);
        property2.setValue(1);

        ArgumentCaptor<PropertyChangeEvent> listenerEvent = ArgumentCaptor.forClass(PropertyChangeEvent.class);
        verify(listener, times(2)).propertyChange(listenerEvent.capture());

        List<PropertyChangeEvent> events = listenerEvent.getAllValues();
        property1.assertEvent(events.get(0), 1);
        property2.assertEvent(events.get(1), 1);

        listenerForwarder.removePropertyChangeListener(listener);

        property1.setValue(2);
        property2.setValue(2);

        verify(listener, times(2)).propertyChange(any(PropertyChangeEvent.class));

        listenerForwarder.checkListenerConsistency();
    }

    @Test
    public void testTwoPropertiesWithDifferentListeners() {
        TestProperty property1 = new TestProperty("my-property1");
        TestProperty property2 = new TestProperty("my-property2");

        SwingPropertyChangeForwarder listenerForwarder = fromProperties(property1, property2);

        PropertyChangeListener listener1 = mock(PropertyChangeListener.class);
        PropertyChangeListener listener2 = mock(PropertyChangeListener.class);

        listenerForwarder.addPropertyChangeListener(property1.name, listener1);
        listenerForwarder.addPropertyChangeListener(property2.name, listener2);

        verifyZeroInteractions(listener1, listener2);

        property1.setValue(1);
        property1.verifyMock(listener1, 1);
        verifyZeroInteractions(listener2);

        property2.setValue(2);
        property1.verifyMock(listener1, 1);
        property2.verifyMock(listener2, 2);

        listenerForwarder.removePropertyChangeListener(listener1);

        property1.setValue(3);
        property2.setValue(4);

        property1.verifyMock(listener1, 1);
        property2.verifyMock(listener2, 2, 4);

        listenerForwarder.checkListenerConsistency();
    }

    @Test
    public void testTwoListeners1() {
        TestProperty property = new TestProperty("my-property");

        SwingPropertyChangeForwarder listenerForwarder = fromProperties(property);

        PropertyChangeListener listener1 = mock(PropertyChangeListener.class);
        listenerForwarder.addPropertyChangeListener(listener1);

        PropertyChangeListener listener2 = mock(PropertyChangeListener.class);
        listenerForwarder.addPropertyChangeListener(listener2);

        verifyZeroInteractions(listener1, listener2);

        property.setValue(1);
        property.verifyMock(listener1, 1);
        property.verifyMock(listener2, 1);

        listenerForwarder.removePropertyChangeListener(listener1);

        property.setValue(2);
        property.verifyMock(listener1, 1);
        property.verifyMock(listener2, 1, 2);

        listenerForwarder.checkListenerConsistency();
    }

    @Test
    public void testTwoListeners2() {
        TestProperty property = new TestProperty("my-property");

        SwingPropertyChangeForwarder listenerForwarder = fromProperties(property);

        PropertyChangeListener listener1 = mock(PropertyChangeListener.class);
        listenerForwarder.addPropertyChangeListener(listener1);

        PropertyChangeListener listener2 = mock(PropertyChangeListener.class);
        listenerForwarder.addPropertyChangeListener(listener2);

        verifyZeroInteractions(listener1, listener2);

        property.setValue(1);
        property.verifyMock(listener1, 1);
        property.verifyMock(listener2, 1);

        listenerForwarder.removePropertyChangeListener(listener2);

        property.setValue(2);
        property.verifyMock(listener1, 1, 2);
        property.verifyMock(listener2, 1);

        listenerForwarder.checkListenerConsistency();
    }

    private static final class TestProperty {
        private final String name;
        private final Object src;
        private final MutableProperty<Integer> property;

        public TestProperty(String name) {
            this.name = name;
            this.src = new Object();
            this.property = PropertyFactory.memProperty(0);
        }

        public void setValue(int value) {
            property.setValue(value);
        }

        public void addTo(SwingPropertyChangeForwarder.Builder builder) {
            builder.addProperty(name, property, src);
        }

        public void verifyMock(PropertyChangeListener listener, int... expectedValues) {
            ArgumentCaptor<PropertyChangeEvent> listenerEvent = ArgumentCaptor.forClass(PropertyChangeEvent.class);
            verify(listener, times(expectedValues.length)).propertyChange(listenerEvent.capture());

            int index = 0;
            for (PropertyChangeEvent event: listenerEvent.getAllValues()) {
                assertEvent(event, expectedValues[index]);
                index++;
            }
        }

        public void assertEvent(PropertyChangeEvent event, int expectedValue) {
            assertEquals("event.newValue", expectedValue, event.getNewValue());
            assertEquals("event.propertyName", name, event.getPropertyName());
            assertEquals("event.source", src, event.getSource());
        }
    }
}
