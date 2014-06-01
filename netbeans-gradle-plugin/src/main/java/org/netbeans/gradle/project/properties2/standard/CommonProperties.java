package org.netbeans.gradle.project.properties2.standard;

import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties2.PropertyValueDef;
import org.netbeans.gradle.project.properties2.ValueMerger;
import org.netbeans.gradle.project.properties2.ValueReference;

public final class CommonProperties {
    @SuppressWarnings("unchecked")
    public static <T> PropertyValueDef<T, T> getIdentityValueDef() {
        return (PropertyValueDef<T, T>)IdentityValueDef.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ValueMerger<T> getParentIfNullValueMerger() {
        return (ValueMerger<T>)ParentIfNullValueMerger.INSTANCE;
    }

    private CommonProperties() {
        throw new AssertionError();
    }

    private enum ParentIfNullValueMerger implements ValueMerger<Object> {
        INSTANCE;

        @Override
        public Object mergeValues(Object child, ValueReference<Object> parent) {
            return child != null ? child : parent.getValue();
        }
    }

    private enum IdentityValueDef implements PropertyValueDef<Object, Object> {
        INSTANCE;

        @Override
        public PropertySource<Object> property(Object valueKey) {
            return PropertyFactory.constSource(valueKey);
        }

        @Override
        public Object getKeyFromValue(Object value) {
            return value;
        }
    }
}
