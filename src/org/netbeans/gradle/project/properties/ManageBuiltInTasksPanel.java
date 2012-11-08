package org.netbeans.gradle.project.properties;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.tasks.BuiltInTasks;
import org.netbeans.gradle.project.view.CustomActionPanel;

@SuppressWarnings("serial")
public class ManageBuiltInTasksPanel extends javax.swing.JPanel {
    private static final Logger LOGGER = Logger.getLogger(ManageBuiltInTasksPanel.class.getName());
    private static final Collator STR_CMP = Collator.getInstance();

    private final ProjectProperties projectProperties;
    private final CustomActionPanel jActionPanel;
    private BuiltInTaskItem lastShownItem;
    private final Map<String, SavedTask> toSaveTasks;

    /**
     * Creates new form ManageBuiltInTasksPanel
     */
    public ManageBuiltInTasksPanel(ProjectProperties properties) {
        this.projectProperties = properties;
        lastShownItem = null;
        toSaveTasks = new HashMap<String, SavedTask>();

        initComponents();
        jActionPanel = new CustomActionPanel();
        jTaskConfigHolder.add(jActionPanel);
        jInheritCheck.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setEnabledDisabledState();
            }
        });

        fillTaskCombo();
        jTaskCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    showSelectedItem();
                }
            }
        });
        showSelectedItem();
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

    private void fillTaskCombo() {
        Set<String> commands = AbstractProjectProperties.getCustomizableCommands();
        List<BuiltInTaskItem> items = new ArrayList<BuiltInTaskItem>(commands.size());
        for (String command: commands) {
            items.add(new BuiltInTaskItem(command));
        }
        Collections.sort(items, new Comparator<BuiltInTaskItem>() {
            @Override
            public int compare(BuiltInTaskItem o1, BuiltInTaskItem o2) {
                return STR_CMP.compare(o1.getDisplayName(), o2.getDisplayName());
            }
        });
        jTaskCombo.setModel(new DefaultComboBoxModel(items.toArray()));
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

    private MutableProperty<PredefinedTask> getTaskProperty(BuiltInTaskItem item) {
        if (item == null) {
            return null;
        }
        return projectProperties.tryGetBuiltInTask(item.getCommand());
    }

    private SavedTask getLastShownTask() {
        if (lastShownItem == null) {
            return null;
        }

        String command = lastShownItem.getCommand();

        String[] rawTaskNames = jActionPanel.getTasks();
        List<PredefinedTask.Name> names;
        if (rawTaskNames.length > 0) {
            names = new ArrayList<PredefinedTask.Name>(rawTaskNames.length);
            for (String name: rawTaskNames) {
                names.add(new PredefinedTask.Name(name, false));
            }
        }
        else {
            SavedTask lastValue = toSaveTasks.get(command);
            if (lastValue == null) {
                names = BuiltInTasks.getDefaultBuiltInTask(command).getTaskNames();
            }
            else {
                names = lastValue.getTaskDef().getTaskNames();
            }
        }

        PredefinedTask resultTask = new PredefinedTask(
                command,
                names,
                Arrays.asList(jActionPanel.getArguments()),
                Arrays.asList(jActionPanel.getJvmArguments()),
                jActionPanel.isNonBlocking());
        return new SavedTask(resultTask, jInheritCheck.isSelected());
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
            MutableProperty<PredefinedTask> taskProperty = getTaskProperty(selectedItem);
            task = taskProperty != null ? taskProperty.getValue() : null;
            defaultValue = taskProperty != null ? taskProperty.isDefault() : false;
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

    private static <ValueType> PropertySource<ValueType> asConst(ValueType value, boolean defaultValue) {
        return new ConstPropertySource<ValueType>(value, defaultValue);
    }

    public void saveModifiedTasks() {
        saveLastShown();

        for (SavedTask task: toSaveTasks.values()) {
            String command = task.getCommand();
            MutableProperty<PredefinedTask> taskProperty = projectProperties.tryGetBuiltInTask(command);
            if (taskProperty != null) {
                if (task.isInherited()) {
                    PredefinedTask defaultTask = BuiltInTasks.getDefaultBuiltInTask(command);
                    taskProperty.setValueFromSource(asConst(defaultTask, true));
                }
                else {
                    taskProperty.setValueFromSource(asConst(task.getTaskDef(), false));
                }
            }
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

        public BuiltInTaskItem(String command) {
            assert command != null;
            this.command = command;
            this.displayName = BuiltInTasks.getDisplayNameOfCommand(command);
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
            hash = 79 * hash + (this.command != null ? this.command.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final BuiltInTaskItem other = (BuiltInTaskItem)obj;
            if ((this.command == null) ? (other.command != null) : !this.command.equals(other.command))
                return false;
            return true;
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

        jTaskCombo = new javax.swing.JComboBox();
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTaskConfigHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTaskCombo, 0, 321, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jInheritCheck)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTaskCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jInheritCheck))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTaskConfigHolder, javax.swing.GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jInheritCheck;
    private javax.swing.JComboBox jTaskCombo;
    private javax.swing.JPanel jTaskConfigHolder;
    // End of variables declaration//GEN-END:variables
}
