package org.netbeans.gradle.project.properties;

import java.awt.Dialog;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.properties.standard.PredefinedTasks;
import org.netbeans.gradle.project.view.CustomActionPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

@SuppressWarnings("serial")
public class ManageTasksPanel extends javax.swing.JPanel {
    private static final Collator STR_CMP = Collator.getInstance();

    private final CustomActionPanel jActionPanel;
    private PredefinedTaskItem currentlyShown;

    private ManageTasksPanel() {
        initComponents();

        jActionPanel = new CustomActionPanel(true, true);
        jTaskSettingsHolder.add(jActionPanel);
        currentlyShown = null;

        showSelected();
        jDefinedTasks.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                showSelected();
            }
        });
    }

    public static ProfileBasedPanel createProfileBasedPanel(final NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        final ManageTasksPanel customPanel = new ManageTasksPanel();
        return ProfileBasedPanel.createPanel(project, customPanel, new ProfileValuesEditorFactory() {
            @Override
            public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery) {
                return customPanel.new PropertyValues(profileQuery);
            }
        });
    }

    private void sortTasks() {
        Object selected = jDefinedTasks.getSelectedValue();

        DefaultListModel<PredefinedTaskItem> model = getModelOfTaskList();
        int elementCount = model.getSize();
        PredefinedTaskItem[] elements = new PredefinedTaskItem[elementCount];
        for (int i = 0; i < elementCount; i++) {
            elements[i] = model.get(i);
        }
        Arrays.sort(elements, new Comparator<PredefinedTaskItem>() {
            @Override
            public int compare(PredefinedTaskItem o1, PredefinedTaskItem o2) {
                String name1 = o1.toString();
                String name2 = o2.toString();
                return STR_CMP.compare(name1, name2);
            }
        });
        for (int i = 0; i < elementCount; i++) {
            model.set(i, elements[i]);
        }
        jDefinedTasks.setSelectedValue(selected, true);
    }

    private DefaultListModel<PredefinedTaskItem> getModelOfTaskList() {
        ListModel<PredefinedTaskItem> model = jDefinedTasks.getModel();
        if (model instanceof DefaultListModel) {
            return (DefaultListModel<PredefinedTaskItem>)model;
        }

        DefaultListModel<PredefinedTaskItem> result = new DefaultListModel<>();
        jDefinedTasks.setModel(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private PredefinedTaskItem getSelectedTask() {
        return jDefinedTasks.getSelectedValue();
    }

    private PredefinedTask getShownTask() {
        if (currentlyShown == null) {
            return null;
        }

        boolean mustExist = jActionPanel.isTasksMustExist();
        String[] rawTaskNames = jActionPanel.getTasks();

        List<PredefinedTask.Name> names;
        if (rawTaskNames.length > 0) {
            names = new ArrayList<>(rawTaskNames.length);
            for (String name: rawTaskNames) {
                names.add(new PredefinedTask.Name(name, mustExist));
            }
        }
        else {
            names = currentlyShown.getTask().getTaskNames();
        }

        return new PredefinedTask(
                currentlyShown.getTask().getDisplayName(),
                names,
                Arrays.asList(jActionPanel.getArguments()),
                Arrays.asList(jActionPanel.getJvmArguments()),
                jActionPanel.isNonBlocking());
    }

    private void updateShownInList() {
        PredefinedTask newTaskDef = getShownTask();
        if (newTaskDef == null) {
            return;
        }

        DefaultListModel<PredefinedTaskItem> listedTasks = getModelOfTaskList();
        int elementCount = listedTasks.getSize();

        for (int i = 0; i < elementCount; i++) {
            @SuppressWarnings("unchecked")
            PredefinedTaskItem current = listedTasks.getElementAt(i);
            if (current == currentlyShown) {
                currentlyShown = new PredefinedTaskItem(newTaskDef);
                listedTasks.set(i, currentlyShown);
                break;
            }
        }
    }

    private void showSelected() {
        int selectedIndex = jDefinedTasks.getSelectedIndex();
        // TODO: disable/enable components depending on the selection
        if (selectedIndex < 0) {
            return;
        }

        updateShownInList();

        @SuppressWarnings("unchecked")
        PredefinedTaskItem selected = getModelOfTaskList().getElementAt(selectedIndex);

        currentlyShown = selected;
        jActionPanel.updatePanel(selected.getTask());
    }

    private static class PredefinedTaskItem {
        private final PredefinedTask task;

        public PredefinedTaskItem(PredefinedTask task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        public boolean isMustExist() {
            for (PredefinedTask.Name name: task.getTaskNames()) {
                if (!name.isMustExist()) {
                    return false;
                }
            }
            return true;
        }

        public PredefinedTask getTask() {
            return task;
        }

        @Override
        public String toString() {
            return task.getDisplayName();
        }
    }

    private final class PropertyValues implements ProfileValuesEditor {
        public final PropertyReference<PredefinedTasks> customTasksRef;
        private PredefinedTasks currentTasks;

        public PropertyValues(ActiveSettingsQuery settings) {
            this.customTasksRef = NbGradleCommonProperties.customTasks(settings);
            this.currentTasks = customTasksRef.tryGetValueWithoutFallback();
        }

        @Override
        public void displayValues() {
            PredefinedTasks customTasks = currentTasks;

            DefaultListModel<PredefinedTaskItem> listModel = getModelOfTaskList();
            listModel.clear();
            if (customTasks != null) {
                for (PredefinedTask task: customTasks.getTasks()) {
                    listModel.addElement(new PredefinedTaskItem(task));
                }
                sortTasks();
            }
        }

        @Override
        public void readFromGui() {
            updateShownInList();

            DefaultListModel<PredefinedTaskItem> listedTasks = getModelOfTaskList();
            int elementCount = listedTasks.getSize();

            List<PredefinedTask> newTasks = new ArrayList<>(elementCount);
            for (int i = 0; i < elementCount; i++) {
                @SuppressWarnings("unchecked")
                PredefinedTaskItem current = listedTasks.getElementAt(i);
                newTasks.add(current.getTask());
            }

            currentTasks = new PredefinedTasks(newTasks);
        }

        @Override
        public void applyValues() {
            customTasksRef.trySetValue(currentTasks != null ? currentTasks : PredefinedTasks.NO_TASKS);
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

        jScrollPane1 = new javax.swing.JScrollPane();
        jDefinedTasks = new javax.swing.JList<PredefinedTaskItem>();
        jTasksCaption = new javax.swing.JLabel();
        jTaskSettingsHolder = new javax.swing.JPanel();
        jAddNewButton = new javax.swing.JButton();
        jRemoveButton = new javax.swing.JButton();

        jDefinedTasks.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jDefinedTasks);

        org.openide.awt.Mnemonics.setLocalizedText(jTasksCaption, org.openide.util.NbBundle.getMessage(ManageTasksPanel.class, "ManageTasksPanel.jTasksCaption.text")); // NOI18N

        jTaskSettingsHolder.setLayout(new java.awt.GridLayout(1, 1));

        org.openide.awt.Mnemonics.setLocalizedText(jAddNewButton, org.openide.util.NbBundle.getMessage(ManageTasksPanel.class, "ManageTasksPanel.jAddNewButton.text")); // NOI18N
        jAddNewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddNewButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jRemoveButton, org.openide.util.NbBundle.getMessage(ManageTasksPanel.class, "ManageTasksPanel.jRemoveButton.text")); // NOI18N
        jRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRemoveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTaskSettingsHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jAddNewButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jRemoveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTasksCaption)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTasksCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jAddNewButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRemoveButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTaskSettingsHolder, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRemoveButtonActionPerformed
        PredefinedTaskItem selected = getSelectedTask();
        if (selected == null) {
            return;
        }

        DefaultListModel<PredefinedTaskItem> listedTasks = getModelOfTaskList();
        int elementCount = listedTasks.getSize();
        for (int i = 0; i < elementCount; i++) {
            @SuppressWarnings("unchecked")
            PredefinedTaskItem current = listedTasks.getElementAt(i);
            if (current == selected) {
                listedTasks.remove(i);
                break;
            }
        }
    }//GEN-LAST:event_jRemoveButtonActionPerformed

    private void jAddNewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddNewButtonActionPerformed
        AddNewTaskPanel panel = new AddNewTaskPanel();

        DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getAddNewTaskDlgTitle(),
                true,
                new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                DialogDescriptor.OK_OPTION,
                DialogDescriptor.BOTTOM_ALIGN,
                null,
                null);
        Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
        dlg.pack();
        dlg.setVisible(true);
        if (dlgDescriptor.getValue() == DialogDescriptor.OK_OPTION) {
            String displayName = panel.getDisplayName();
            if (!displayName.isEmpty()) {
                PredefinedTask newTask = new PredefinedTask(
                        displayName,
                        Collections.singletonList(new PredefinedTask.Name("tasks", true)),
                        Collections.<String>emptyList(),
                        Collections.<String>emptyList(),
                        false);
                PredefinedTaskItem item = new PredefinedTaskItem(newTask);
                getModelOfTaskList().addElement(item);
                jDefinedTasks.setSelectedValue(item, true);
                sortTasks();
            }
        }
    }//GEN-LAST:event_jAddNewButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddNewButton;
    private javax.swing.JList<PredefinedTaskItem> jDefinedTasks;
    private javax.swing.JButton jRemoveButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel jTaskSettingsHolder;
    private javax.swing.JLabel jTasksCaption;
    // End of variables declaration//GEN-END:variables
}
