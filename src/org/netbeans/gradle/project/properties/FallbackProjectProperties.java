package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;

public final class FallbackProjectProperties
extends
        AbstractProjectProperties {
    private static final Logger LOGGER = Logger.getLogger(FallbackProjectProperties.class.getName());

    private final ProjectProperties mainProperties;
    private final ProjectProperties defaultProperties;

    private final MutableProperty<String> sourceLevel;
    private final MutableProperty<JavaPlatform> platform;
    private final MutableProperty<Charset> sourceEncoding;
    private final MutableProperty<List<PredefinedTask>> commonTasks;

    public FallbackProjectProperties(ProjectProperties mainProperties, ProjectProperties defaultProperties) {
        if (mainProperties == null) throw new NullPointerException("mainProperties");
        if (defaultProperties == null) throw new NullPointerException("defaultProperties");

        this.mainProperties = mainProperties;
        this.defaultProperties = defaultProperties;

        this.sourceLevel = new FallbackProperty<String>(
                mainProperties.getSourceLevel(),
                defaultProperties.getSourceLevel());

        this.platform = new FallbackProperty<JavaPlatform>(
                mainProperties.getPlatform(),
                defaultProperties.getPlatform());

        this.sourceEncoding = new FallbackProperty<Charset>(
                mainProperties.getSourceEncoding(),
                defaultProperties.getSourceEncoding());

        this.commonTasks = new ListMergerProperty<PredefinedTask>(
                mainProperties.getCommonTasks(),
                defaultProperties.getCommonTasks());
    }

    @Override
    public MutableProperty<String> getSourceLevel() {
        return sourceLevel;
    }

    @Override
    public MutableProperty<JavaPlatform> getPlatform() {
        return platform;
    }

    @Override
    public MutableProperty<Charset> getSourceEncoding() {
        return sourceEncoding;
    }

    @Override
    public MutableProperty<List<PredefinedTask>> getCommonTasks() {
        return commonTasks;
    }

    @Override
    public MutableProperty<PredefinedTask> tryGetBuiltInTask(String command) {
        MutableProperty<PredefinedTask> mainProperty = mainProperties.tryGetBuiltInTask(command);
        MutableProperty<PredefinedTask> defaultProperty = defaultProperties.tryGetBuiltInTask(command);
        if (mainProperty == null) {
            if (defaultProperty == null) {
                // We must ignore because otherwise someone trying to set the
                // property would set the defaultProperty which should not
                // happen.
                LOGGER.warning("Main property is null but the default is not, ignoring the default.");
            }
            return null;
        }
        if (defaultProperty == null) {
            return mainProperty;
        }

        return new FallbackProperty<PredefinedTask>(mainProperty, defaultProperty);
    }

    private static class ListMergerProperty<ElementType>
    implements
            MutableProperty<List<ElementType>> {

        private final MutableProperty<List<ElementType>> mainValue;
        private final MutableProperty<List<ElementType>> defaultValue;

        public ListMergerProperty(MutableProperty<List<ElementType>> mainValue, MutableProperty<List<ElementType>> defaultValue) {
            if (mainValue == null) throw new NullPointerException("mainValue");
            if (defaultValue == null) throw new NullPointerException("defaultValue");

            this.mainValue = mainValue;
            this.defaultValue = defaultValue;
        }

        @Override
        public void setValueFromSource(PropertySource<? extends List<ElementType>> source) {
            mainValue.setValueFromSource(source);
        }

        @Override
        public void setValue(List<ElementType> value) {
            mainValue.setValue(value);
        }

        @Override
        public List<ElementType> getValue() {
            List<ElementType> mainList = mainValue.getValue();
            List<ElementType> defaultList = defaultValue.getValue();

            List<ElementType> result = new ArrayList<ElementType>(mainList.size() + defaultList.size());
            result.addAll(mainList);
            result.addAll(defaultList);
            return result;
        }

        @Override
        public boolean isDefault() {
            return mainValue.isDefault() && defaultValue.isDefault();
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            if (listener == null) throw new NullPointerException("listener");

            mainValue.addChangeListener(listener);
            defaultValue.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            if (listener == null) throw new NullPointerException("listener");

            defaultValue.removeChangeListener(listener);
            mainValue.removeChangeListener(listener);
        }
    }
}
