package org.netbeans.gradle.project.properties.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import org.netbeans.gradle.project.NbStrings;

public final class EnumCombo<EnumType extends Enum<EnumType>> {
    private static final Collator STR_CMP = Collator.getInstance();

    private final Class<EnumType> enumType;
    private final EnumType defaultValue;
    private final JComboBox<Item<EnumType>> combo;

    public EnumCombo(
            Class<EnumType> enumType,
            EnumType defaultValue,
            JComboBox<Item<EnumType>> combo) {
        this.enumType = Objects.requireNonNull(enumType, "enumType");
        this.defaultValue = defaultValue;
        this.combo = Objects.requireNonNull(combo, "combo");

        fillCombo();
    }

    public void setSelectedValue(EnumType value) {
        combo.setSelectedItem(new Item<>(value));
    }

    public EnumType getSelectedValue() {
        Item<?> selected = (Item<?>)combo.getSelectedItem();
        return selected != null
                ? enumType.cast(selected.getValue())
                : defaultValue;
    }

    private void fillCombo() {
        EnumType[] values = enumType.getEnumConstants();
        List<Item<EnumType>> entries = new ArrayList<>(values.length);
        for (EnumType value: values) {
            entries.add(new Item<>(value));
        }

        entries.sort(Comparator.comparing(Object::toString, STR_CMP::compare));

        combo.removeAllItems();
        for (Item<EnumType> entry: entries) {
            combo.addItem(entry);
        }
    }

    public static final class Item<EnumType extends Enum<EnumType>> {
        private final EnumType value;
        private final String displayName;

        public Item(EnumType value) {
            this.value = value;
            this.displayName = NbStrings.getEnumDisplayValue(value);
        }

        public EnumType getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return 445 + Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Item<?> other = (Item<?>)obj;
            return this.value == other.value;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
