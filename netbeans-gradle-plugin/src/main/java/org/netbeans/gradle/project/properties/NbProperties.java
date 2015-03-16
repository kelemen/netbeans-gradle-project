package org.netbeans.gradle.project.properties;

import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;

public final class NbProperties {
    public static PropertySource<Boolean> between(
            final PropertySource<Integer> wrapped,
            final int minValue,
            final int maxValue) {

        return PropertyFactory.convert(wrapped, new ValueConverter<Integer, Boolean>() {
            @Override
            public Boolean convert(Integer input) {
                if (input == null) return null;
                return input <= maxValue && input >= minValue;
            }
        });
    }

    public static PropertySource<Boolean> greaterThanOrEqual(
            final PropertySource<Integer> wrapped,
            final int value) {
        return between(wrapped, value, Integer.MAX_VALUE);
    }

    public static PropertySource<Boolean> lessThanOrEqual(
            final PropertySource<Integer> wrapped,
            final int value) {
        return between(wrapped, Integer.MIN_VALUE, value);
    }
}
