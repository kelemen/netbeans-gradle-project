package org.netbeans.gradle.project.properties.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.utils.LazyValues;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.api.config.ValueMerger;
import org.netbeans.gradle.project.api.config.ValueReference;

public final class CustomVariablesProperty {
    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("custom-variables");

    private static final String CONFIG_KEY_VAR = "var";
    private static final String CONFIG_KEY_VAR_NAME = "name";
    private static final String CONFIG_KEY_VAR_VALUE = "value";

    public static final PropertyDef<CustomVariables, CustomVariables> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<CustomVariables, CustomVariables> createPropertyDef() {
        PropertyDef.Builder<CustomVariables, CustomVariables> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(CustomVariablesEncodingDef.INSTANCE);
        result.setValueDef(CustomVariablesValueDef.INSTANCE);
        result.setValueMerger(CustomVariablesMerger.INSTANCE);
        return result.create();
    }

    private enum CustomVariablesEncodingDef implements PropertyKeyEncodingDef<CustomVariables> {
        INSTANCE;

        @Override
        public CustomVariables decode(ConfigTree config) {
            List<ConfigTree> varsConfig = config.getChildTrees(CONFIG_KEY_VAR);
            List<CustomVariable> vars = new ArrayList<>(varsConfig.size());
            for (ConfigTree var: varsConfig) {
                String name = var.getChildTree(CONFIG_KEY_VAR_NAME).getValue(null);
                String value = var.getChildTree(CONFIG_KEY_VAR_VALUE).getValue(null);
                if (name != null && value != null) {
                    vars.add(new CustomVariable(name, value));
                }
            }
            return new MemCustomVariables(vars);
        }

        @Override
        public ConfigTree encode(CustomVariables value) {
            if (value.isEmpty()) {
                return ConfigTree.EMPTY;
            }

            ConfigTree.Builder result = new ConfigTree.Builder();
            for (CustomVariable var: value.getVariables()) {
                ConfigTree.Builder varConfig = result.addChildBuilder(CONFIG_KEY_VAR);
                varConfig.addChildBuilder(CONFIG_KEY_VAR_NAME).setValue(var.getName());
                varConfig.addChildBuilder(CONFIG_KEY_VAR_VALUE).setValue(var.getValue());
            }
            return result.create();
        }
    }

    private enum CustomVariablesMerger implements ValueMerger<CustomVariables> {
        INSTANCE;

        @Override
        public CustomVariables mergeValues(CustomVariables child, ValueReference<CustomVariables> parent) {
            if (child == null) {
                return parent.getValue();
            }

            return new MergedCustomVariables(child, parent);
        }
    }

    private static final class MergedCustomVariables implements CustomVariables {
        private final CustomVariables child;
        private final Supplier<CustomVariables> parentCache;

        public MergedCustomVariables(CustomVariables child, ValueReference<CustomVariables> parentRef) {
            assert child != null;
            assert parentRef != null;

            this.child = child;
            this.parentCache = LazyValues.lazyValue(() -> {
                CustomVariables result = parentRef.getValue();
                return result != null ? result : MemCustomVariables.EMPTY;
            });
        }

        private CustomVariables getParent() {
            return parentCache.get();
        }

        @Override
        public String tryGetValue(String name) {
            String result = child.tryGetValue(name);
            return result != null
                    ? result
                    : getParent().tryGetValue(name);
        }

        @Override
        public Collection<CustomVariable> getVariables() {
            Map<String, CustomVariable> vars = new HashMap<>();
            for (CustomVariable var: child.getVariables()) {
                vars.put(var.getName(), var);
            }
            for (CustomVariable var: getParent().getVariables()) {
                vars.put(var.getName(), var);
            }
            return vars.values();
        }

        @Override
        public boolean isEmpty() {
            return child.isEmpty() && getParent().isEmpty();
        }
    }

    private enum CustomVariablesValueDef implements PropertyValueDef<CustomVariables, CustomVariables> {
        INSTANCE;

        @Override
        public PropertySource<CustomVariables> property(CustomVariables valueKey) {
            return PropertyFactory.constSource(valueKey);
        }

        @Override
        public CustomVariables getKeyFromValue(CustomVariables value) {
            return value == null || value.isEmpty() ? null : value;
        }
    }
}
