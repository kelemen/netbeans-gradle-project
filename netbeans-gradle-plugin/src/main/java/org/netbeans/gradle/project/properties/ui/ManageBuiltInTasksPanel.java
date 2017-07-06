package org.netbeans.gradle.project.properties.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsCategory;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.standard.BuiltInTasks;
import org.netbeans.gradle.project.properties.standard.BuiltInTasksProperty;
import org.netbeans.gradle.project.properties.standard.PredefinedTasks;
import org.netbeans.gradle.project.tasks.DefaultBuiltInTasks;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.view.CustomActionPanel;

@SuppressWarnings("serial")
public class ManageBuiltInTasksPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final Logger LOGGER = Logger.getLogger(ManageBuiltInTasksPanel.class.getName());

    private static final CustomizerCategoryId CATEGORY_ID = new CustomizerCategoryId(
            ManageBuiltInTasksPanel.class.getName() + ".settings",
            NbStrings.getManageBuiltInTasksTitle());

    private final NbGradleProject project;
    private final CustomActionPanel jActionPanel;
    private BuiltInTaskItem lastShownItem;
    private final Map<String, SavedTask> toSaveTasks;
    private PropertyRefs currentEditor;

    private ManageBuiltInTasksPanel(NbGradleProject project) {
        this.project = project;
        this.lastShownItem = null;
        this.toSaveTasks = new HashMap<>();
        this.currentEditor = null;

        initComponents();
        jActionPanel = new CustomActionPanel();
        jTaskConfigHolder.add(jActionPanel);
        jInheritCheck.getModel().addChangeListener((ChangeEvent e) -> {
            setEnabledDisabledState();
        });

        fillTaskCombo();
        jTaskCombo.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                showSelectedItem();
            }
        });
        showSelectedItem();
    }

    public static ProfileBasedSettingsCategory createSettingsCategory(final NbGradleProject project) {
        Objects.requireNonNull(project, "project");

        return new ProfileBasedSettingsCategory(CATEGORY_ID, () -> ManageBuiltInTasksPanel.createSettingsPage(project));
    }

    public static ProfileBasedSettingsPage createSettingsPage(NbGradleProject project) {
        ManageBuiltInTasksPanel result = new ManageBuiltInTasksPanel(project);
        return new ProfileBasedSettingsPage(result, result);
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(project, profileQuery);
    }

    private void setEnableChildrenRecursive(Component component, boolean enabled) {
        if (component instanceof Container) {
            for (Component subComponent: ((Container)component).getComponents()) {
                if (subComponent instanceof JPanel
                        || subComponent instanceof JScrollPane
                        || subComponent instanceof JViewport) {
                    setEnableChildrenRecursive(subComponent, enabled);
                }
                else {
                    subComponent.setEnabled(enabled);
                }
            }
        }
    }

    private void setEnabledDisabledState() {
        setEnableChildrenRecursive(jActionPanel, !jInheritCheck.isSelected());
    }

    private String getDisplayNameOfCommand(String command) {
        return DefaultBuiltInTasks.getDisplayNameOfCommand(project, command);
    }

    private void fillTaskCombo() {
        Set<String> commands = project.getMergedCommandQuery().getSupportedCommands();
        List<BuiltInTaskItem> items = new ArrayList<>(commands.size());
        for (String command: commands) {
            items.add(new BuiltInTaskItem(command, getDisplayNameOfCommand(command)));
        }

        items.sort(Comparator.comparing(BuiltInTaskItem::getDisplayName, StringUtils.STR_CMP::compare));

        jTaskCombo.setModel(new DefaultComboBoxModel<>(items.toArray(new BuiltInTaskItem[items.size()])));
        jTaskCombo.getModel().setSelectedItem(items.get(0));
    }

    private BuiltInTaskItem getSelectedItem() {
        Object selected = jTaskCombo.getSelectedItem();
        if (selected instanceof BuiltInTaskItem) {
            return (BuiltInTaskItem)selected;
        }

        if (selected == null) {
            LOGGER.warning("There is no selected built-in task.");
        }
        else {
            LOGGER.log(Level.WARNING, "Task combo contains item with invalid type: {0}", selected.getClass().getName());
        }
        return null;
    }

    private PropertyReference<BuiltInTasks> tryGetPropertyRef() {
        return currentEditor != null ? currentEditor.builtInTasksRef : null;
    }

    private PredefinedTask tryGetValueWithoutFallback(BuiltInTaskItem item) {
        PropertyReference<BuiltInTasks> propertyRef = tryGetPropertyRef();

        if (item == null || propertyRef == null) {
            return null;
        }

        BuiltInTasks builtInTasks = propertyRef.tryGetValueWithoutFallback();
        return builtInTasks != null ? builtInTasks.tryGetByCommand(item.command) : null;
    }

    private PredefinedTask tryGetActiveValue(BuiltInTaskItem item) {
        PropertyReference<BuiltInTasks> propertyRef = tryGetPropertyRef();

        if (item == null || propertyRef == null) {
            return null;
        }

        BuiltInTasks builtInTasks = propertyRef.getActiveValue();
        return builtInTasks != null ? builtInTasks.tryGetByCommand(item.command) : null;
    }

    private PredefinedTask getCurrentValue(String command) {
        PropertyReference<BuiltInTasks> propertyRef = tryGetPropertyRef();

        BuiltInTasks builtInTasks = propertyRef != null ? propertyRef.getActiveValue() : null;
        PredefinedTask result = builtInTasks != null ? builtInTasks.tryGetByCommand(command) : null;

        if (result == null) {
            result = new PredefinedTask(command,
                    Arrays.asList(new PredefinedTask.Name("tasks", false)),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    true);
        }
        return result;
    }

    private SavedTask getLastShownTask() {
        if (lastShownItem == null) {
            return null;
        }

        final String command = lastShownItem.getCommand();
        jActionPanel.setTasksMustExist(false);
        PredefinedTask resultTask = jActionPanel.tryGetPredefinedTask(command, () -> {
            SavedTask lastValue = toSaveTasks.get(command);
            if (lastValue == null) {
                return getCurrentValue(command).getTaskNames();
            }
            else {
                return lastValue.getTaskDef().getTaskNames();
            }
        });

        return resultTask != null ? new SavedTask(resultTask, jInheritCheck.isSelected()) : null;
    }

    private void saveLastShown() {
        SavedTask lastShownTask = getLastShownTask();
        if (lastShownTask == null) {
            return;
        }

        toSaveTasks.put(lastShownTask.getCommand(), lastShownTask);
    }

    private void showSelectedItem() {
        BuiltInTaskItem selectedItem = getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        PredefinedTask task;
        boolean defaultValue;

        SavedTask savedTask = toSaveTasks.get(selectedItem.getCommand());
        if (savedTask != null) {
            task = savedTask.getTaskDef();
            defaultValue = savedTask.isInherited();
        }
        else {
            task = tryGetActiveValue(selectedItem);
            defaultValue = tryGetValueWithoutFallback(selectedItem) == null;
        }

        if (task == null) {
            return;
        }

        saveLastShown();
        lastShownItem = null;

        jActionPanel.updatePanel(task);
        jInheritCheck.setSelected(defaultValue);

        lastShownItem = selectedItem;
        setEnabledDisabledState();
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<BuiltInTasks> builtInTasksRef;

        public PropertyRefs(NbGradleProject ownerProject, ActiveSettingsQuery settingsQuery) {
            this.builtInTasksRef = NbGradleCommonProperties.builtInTasks(ownerProject, settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, ManageBuiltInTasksPanel.this);
        }
    }

    private final class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;
        private final Map<String, SavedTask> modifiedCommands;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.modifiedCommands = new HashMap<>();
        }

        public StoredSettingsImpl(PropertyRefs properties, ManageBuiltInTasksPanel panel) {
            this.properties = properties;

            panel.saveLastShown();
            this.modifiedCommands = new HashMap<>(panel.toSaveTasks);
        }

        @Override
        public void displaySettings() {
            ManageBuiltInTasksPanel.this.currentEditor = properties;

            toSaveTasks.clear();
            toSaveTasks.putAll(modifiedCommands);

            showSelectedItem();
        }

        @Override
        public void saveSettings() {
            PropertyReference<BuiltInTasks> builtInTasksRef = properties.builtInTasksRef;
            BuiltInTasks currentTasks = builtInTasksRef.tryGetValueWithoutFallback();
            PredefinedTasks currentTaskList = currentTasks != null
                    ? currentTasks.getAllTasks()
                    : PredefinedTasks.NO_TASKS;

            Map<String, PredefinedTask> taskMap = CollectionUtils.newLinkedHashMap(currentTaskList.getTasks().size());
            for (PredefinedTask task: currentTaskList.getTasks()) {
                String command = task.getDisplayName();
                taskMap.put(command, task);
            }

            for (SavedTask task: modifiedCommands.values()) {
                if (task.isInherited()) {
                    taskMap.remove(task.getCommand());
                }
                else {
                    taskMap.put(task.getCommand(), task.getTaskDef());
                }
            }

            builtInTasksRef.setValue(BuiltInTasksProperty.createValue(taskMap.values()));
        }
    }

    private static class SavedTask {
        private final PredefinedTask taskDef;
        private final boolean inherited;

        public SavedTask(PredefinedTask taskDef, boolean inherited) {
            assert taskDef != null;
            this.taskDef = taskDef;
            this.inherited = inherited;
        }

        public String getCommand() {
            return taskDef.getDisplayName();
        }

        public PredefinedTask getTaskDef() {
            return taskDef;
        }

        public boolean isInherited() {
            return inherited;
        }
    }

    private static class BuiltInTaskItem {
        private final String command;
        private final String displayName;

        public BuiltInTaskItem(String command, String displayName) {
            assert command != null;
            assert displayName != null;

            this.command = command;
            this.displayName = displayName;
        }

        public String getCommand() {
            return command;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + Objects.hashCode(command);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final BuiltInTaskItem other = (BuiltInTaskItem)obj;
            return Objects.equals(this.command, other.command);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTaskCombo = new javax.swing.JComboBox<>();
        jInheritCheck = new javax.swing.JCheckBox();
        jTaskConfigHolder = new javax.swing.JPanel();

        org.openide.awt.Mnemonics.setLocalizedText(jInheritCheck, org.openide.util.NbBundle.getMessage(ManageBuiltInTasksPanel.class, "ManageBuiltInTasksPanel.jInheritCheck.text")); // NOI18N

        jTaskConfigHolder.setLayout(new java.awt.GridLayout(1, 1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTaskCombo, 0, 321, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jInheritCheck)
                .addContainerGap())
            .addComponent(jTaskConfigHolder, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTaskCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jInheritCheck))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTaskConfigHolder, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jInheritCheck;
    private javax.swing.JComboBox<BuiltInTaskItem> jTaskCombo;
    private javax.swing.JPanel jTaskConfigHolder;
    // End of variables declaration//GEN-END:variables
}
