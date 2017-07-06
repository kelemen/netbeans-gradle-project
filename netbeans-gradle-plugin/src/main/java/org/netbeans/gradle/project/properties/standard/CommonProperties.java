package org.netbeans.gradle.project.properties.standard;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.api.config.ValueMerger;
import org.netbeans.gradle.project.api.config.ValueReference;

public final class CommonProperties {
    private static final Logger LOGGER = Logger.getLogger(EnumKeyEncodingDef.class.getName());

    private static final String SAVE_FILE_NAME_SEPARATOR = "/";

    @SuppressWarnings("unchecked")
    public static <T> PropertyValueDef<T, T> getIdentityValueDef() {
        return (PropertyValueDef<T, T>)IdentityValueDef.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ValueMerger<T> getParentIfNullValueMerger() {
        return (ValueMerger<T>)ParentIfNullValueMerger.INSTANCE;
    }

    public static PropertyKeyEncodingDef<String> getIdentityKeyEncodingDef() {
        return IdentityKeyEncodingDef.INSTANCE;
    }

    public static <T extends Enum<T>> PropertyKeyEncodingDef<T> enumKeyEncodingDef(final Class<T> enumType) {
        return new EnumKeyEncodingDef<>(enumType);
    }

    public static PropertyKeyEncodingDef<Integer> intKeyEncodingDef() {
        return IntKeyEncodingDef.INSTANCE;
    }

    public static PropertyKeyEncodingDef<List<String>> stringListEncodingDef() {
        return StringListKeyEncodingDef.DEFAULT;
    }

    public static PropertyKeyEncodingDef<List<String>> stringListEncodingDef(String entryName) {
        return new StringListKeyEncodingDef(entryName);
    }

    public static PropertyKeyEncodingDef<ConfigTree> getIdentityTreeKeyEncodingDef() {
        return IdentityTreeKeyEncodingDef.INSTANCE;
    }

    public static <T> ValueMerger<List<T>> listValueMerger() {
        return new ListValueMerger<>();
    }

    public static Path tryReadFilePath(String normalizedPath) {
        if (normalizedPath == null) {
            return null;
        }

        String separator = FileSystems.getDefault().getSeparator();
        String nativePath = normalizedPath.replace(SAVE_FILE_NAME_SEPARATOR, separator);
        return Paths.get(nativePath);
    }

    public static String normalizeFilePath(Path file) {
        String result = file.toString();
        String separator = file.getFileSystem().getSeparator();
        return result.replace(separator, SAVE_FILE_NAME_SEPARATOR);
    }

    public static PropertyDef<?, File> defineFileProperty(String... keyPath) {
        PropertyDef.Builder<String, File> result = new PropertyDef.Builder<>(ConfigPath.fromKeys(keyPath));
        result.setValueDef(FileValueDef.INSTANCE);
        result.setKeyEncodingDef(getIdentityKeyEncodingDef());
        return result.create();
    }

    public static <T extends Enum<T>> PropertyDef<?, T> defineEnumProperty(Class<T> enumType, String... keyPath) {
        PropertyDef.Builder<T, T> result = new PropertyDef.Builder<>(ConfigPath.fromKeys(keyPath));
        result.setValueDef(CommonProperties.<T>getIdentityValueDef());
        result.setKeyEncodingDef(enumKeyEncodingDef(enumType));
        return result.create();
    }

    public static PropertyDef<?, Boolean> defineBooleanProperty(String... keyPath) {
        PropertyDef.Builder<Boolean, Boolean> result = new PropertyDef.Builder<>(ConfigPath.fromKeys(keyPath));
        result.setValueDef(CommonProperties.<Boolean>getIdentityValueDef());
        result.setKeyEncodingDef(BooleanKeyEncodingDef.INSTANCE);
        return result.create();
    }

    public static PropertyDef<?, Integer> defineIntProperty(String... keyPath) {
        PropertyDef.Builder<Integer, Integer> result = new PropertyDef.Builder<>(ConfigPath.fromKeys(keyPath));
        result.setValueDef(CommonProperties.<Integer>getIdentityValueDef());
        result.setKeyEncodingDef(IntKeyEncodingDef.INSTANCE);
        return result.create();
    }

    public static PropertyDef<?, String> defineStringProperty(String... keyPath) {
        PropertyDef.Builder<String, String> result = new PropertyDef.Builder<>(ConfigPath.fromKeys(keyPath));
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setKeyEncodingDef(getIdentityKeyEncodingDef());
        return result.create();
    }

    public static PropertyDef<?, List<String>> defineStringListProperty(String... keyPath) {
        PropertyDef.Builder<List<String>, List<String>> result = new PropertyDef.Builder<>(ConfigPath.fromKeys(keyPath));
        result.setValueDef(CommonProperties.<List<String>>getIdentityValueDef());
        result.setKeyEncodingDef(stringListEncodingDef());
        result.setValueMerger(CommonProperties.<String>listValueMerger());
        return result.create();
    }

    private enum IdentityTreeKeyEncodingDef implements PropertyKeyEncodingDef<ConfigTree> {
        INSTANCE;

        @Override
        public ConfigTree decode(ConfigTree config) {
            return config;
        }

        @Override
        public ConfigTree encode(ConfigTree value) {
            return value;
        }
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

    private enum IdentityKeyEncodingDef implements PropertyKeyEncodingDef<String> {
        INSTANCE;

        @Override
        public String decode(ConfigTree config) {
            return config.getValue(null);
        }

        @Override
        public ConfigTree encode(String value) {
            return ConfigTree.singleValue(value);
        }
    }

    private enum BooleanKeyEncodingDef implements PropertyKeyEncodingDef<Boolean> {
        INSTANCE;

        private static final String TRUE_STR = Boolean.TRUE.toString();
        private static final String FALSE_STR = Boolean.FALSE.toString();

        @Override
        public Boolean decode(ConfigTree config) {
            String strValue = config.getValue(null);
            if (TRUE_STR.equalsIgnoreCase(strValue)) {
                return true;
            }
            if (FALSE_STR.equalsIgnoreCase(strValue)) {
                return false;
            }
            return null;
        }

        @Override
        public ConfigTree encode(Boolean value) {
            return ConfigTree.singleValue(value.toString());
        }
    }

    private static final class StringListKeyEncodingDef implements PropertyKeyEncodingDef<List<String>> {
        public static final StringListKeyEncodingDef DEFAULT = new StringListKeyEncodingDef("entry");

        private final String entryName;

        public StringListKeyEncodingDef(String entryName) {
            this.entryName = Objects.requireNonNull(entryName, "entryName");
        }

        @Override
        public List<String> decode(ConfigTree config) {
            List<ConfigTree> entries = config.getChildTrees(entryName);
            int entryCount = entries.size();
            if (entryCount == 0) {
                return Collections.emptyList();
            }

            List<String> result = new ArrayList<>(entryCount);
            for (ConfigTree entry: entries) {
                result.add(entry.getValue(""));
            }
            return Collections.unmodifiableList(result);
        }

        @Override
        public ConfigTree encode(List<String> value) {
            if (value.isEmpty()) {
                return ConfigTree.EMPTY;
            }

            ConfigTree.Builder result = new ConfigTree.Builder();
            for (String entry: value) {
                result.addChildBuilder(entryName).setValue(entry);
            }
            return result.create();
        }
    }

    private enum IntKeyEncodingDef implements PropertyKeyEncodingDef<Integer> {
        INSTANCE;

        @Override
        public Integer decode(ConfigTree config) {
            String strValue = config.getValue("").trim();
            if (strValue.isEmpty()) {
                return null;
            }

            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public ConfigTree encode(Integer value) {
            return ConfigTree.singleValue(value.toString());
        }
    }

    private static final class EnumKeyEncodingDef<T extends Enum<T>> implements PropertyKeyEncodingDef<T> {
        private final Class<T> enumType;
        private final Map<String, T> byNameValues;

        public EnumKeyEncodingDef(Class<T> enumType) {
            this.enumType = Objects.requireNonNull(enumType, "enumType");

            T[] allValues = enumType.getEnumConstants();
            byNameValues = CollectionsEx.newHashMap(allValues.length);
            for (T value: allValues) {
                byNameValues.put(value.name().toUpperCase(Locale.ROOT), value);
            }
        }

        @Override
        public T decode(ConfigTree config) {
            String strValue = config.getValue("").trim();
            if (strValue.isEmpty()) {
                return null;
            }

            T result = byNameValues.get(strValue.toUpperCase(Locale.ROOT));
            if (result == null) {
                LOGGER.log(Level.INFO,
                        "Illegal enum value for config: {0} expected an instance of {1}",
                        new Object[]{strValue, enumType.getSimpleName()});
            }
            return result;
        }

        @Override
        public ConfigTree encode(T value) {
            return ConfigTree.singleValue(value.name());
        }
    }

    private enum FileValueDef implements PropertyValueDef<String, File> {
        INSTANCE;

        @Override
        public PropertySource<File> property(String valueKey) {
            return PropertyFactory.constSource(valueKey != null ? new File(valueKey) : null);
        }

        @Override
        public String getKeyFromValue(File value) {
            return value != null ? value.getPath() : null;
        }
    }

    private static class ListValueMerger<T> implements ValueMerger<List<T>> {
        public ListValueMerger() {
        }

        @Override
        public List<T> mergeValues(List<T> child, ValueReference<List<T>> parent) {
            List<T> parentValue = parent.getValue();
            if (parentValue == null || parentValue.isEmpty()) {
                return child;
            }

            if (child == null || child.isEmpty()) {
                return parentValue;
            }

            List<T> result = new ArrayList<>(child.size() + parentValue.size());
            result.addAll(child);
            result.addAll(parentValue);
            return result;
        }
    }

    private CommonProperties() {
        throw new AssertionError();
    }
}
