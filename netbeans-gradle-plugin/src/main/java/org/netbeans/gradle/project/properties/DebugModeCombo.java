package org.netbeans.gradle.project.properties;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.properties.global.DebugMode;

public class DebugModeCombo {
    private static final Collator STR_CMP = Collator.getInstance();

    private final JComboBox<Item> debugModeCombo;

    public DebugModeCombo(JComboBox<Item> debugModeCombo) {
        ExceptionHelper.checkNotNullArgument(debugModeCombo, "debugModeCombo");
        this.debugModeCombo = debugModeCombo;
        fillDebugModeCombo();
    }

    public void setSelectedDebugMode(DebugMode debugMode) {
        debugModeCombo.setSelectedItem(new Item(debugMode));
    }

    public DebugMode getSelectedDebugMode() {
        Item selected = (Item)debugModeCombo.getSelectedItem();
        return selected != null
                ? selected.getDebugMode()
                : DebugMode.DEBUGGER_ATTACHES;
    }

    private void fillDebugModeCombo() {
        DebugMode[] debugModes = DebugMode.values();
        List<Item> entries = new ArrayList<>(debugModes.length);
        for (DebugMode debugMode: debugModes) {
            entries.add(new Item(debugMode));
        }
        Collections.sort(entries, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                return STR_CMP.compare(a.toString(), b.toString());
            }
        });

        debugModeCombo.removeAllItems();
        for (Item entry: entries) {
            debugModeCombo.addItem(entry);
        }
    }

    public static final class Item {
        private final DebugMode debugMode;
        private final String displayName;

        public Item(DebugMode debugMode) {
            this.debugMode = debugMode;
            this.displayName = NbStrings.getDebugMode(debugMode);
        }

        public DebugMode getDebugMode() {
            return debugMode;
        }

        @Override
        public int hashCode() {
            return 445 + Objects.hashCode(debugMode);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Item other = (Item)obj;
            return this.debugMode == other.debugMode;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
