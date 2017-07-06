package org.netbeans.gradle.project.view;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JTextArea;
import org.jtrim2.property.BoolProperties;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.validate.Validators;

@SuppressWarnings("serial") // Don't care about serialization
public class CustomActionPanel extends javax.swing.JPanel {
    public CustomActionPanel() {
        this(true);
    }

    public CustomActionPanel(boolean showNonBlockingCheckBox) {
        this(showNonBlockingCheckBox, false);
    }

    public CustomActionPanel(boolean showNonBlockingCheckBox, boolean showTaskMustExistCheckBox) {
        initComponents();

        if (!showNonBlockingCheckBox) {
            jNonBlockingCheck.setVisible(false);
        }
        if (!showTaskMustExistCheckBox) {
            jMustExistCheck.setVisible(false);
        }
    }

    public PropertySource<Boolean> validInput() {
        PropertySource<String> taskNames = Validators.trimmedText(jTasksEdit);
        PropertySource<Boolean> emptyNames = BoolProperties.equalsWithConst(taskNames, "");
        return BoolProperties.not(emptyNames);
    }

    public void setNonBlocking(boolean value) {
        jNonBlockingCheck.setSelected(value);
    }

    public void setTasksMustExist(boolean value) {
        jMustExistCheck.setSelected(value);
    }

    public void updatePanel(PredefinedTask task) {
        StringBuilder tasks = new StringBuilder(1024);

        boolean mustExist = false;
        for (PredefinedTask.Name name: task.getTaskNames()) {
            tasks.append(name.getName());
            tasks.append(' ');
            if (name.isMustExist()) {
                mustExist = true;
            }
        }
        jTasksEdit.setText(tasks.toString());

        StringBuilder arguments = new StringBuilder(1024);
        for (String arg: task.getArguments()) {
            arguments.append(arg);
            arguments.append('\n');
        }
        jArgsTextArea.setText(arguments.toString());

        StringBuilder jvmArguments = new StringBuilder(1024);
        for (String arg: task.getJvmArguments()) {
            jvmArguments.append(arg);
            jvmArguments.append('\n');
        }
        jJvmArgsTextArea.setText(jvmArguments.toString());
        jNonBlockingCheck.setSelected(task.isNonBlocking());
        jMustExistCheck.setSelected(mustExist);
    }

    public GradleCommandTemplate tryGetGradleCommand(String displayName) {
        List<String> tasks = getTasks();
        if (tasks.isEmpty()) {
            return null;
        }

        GradleCommandTemplate.Builder builder = new GradleCommandTemplate.Builder(
                displayName != null ? displayName : "",
                tasks);

        builder.setArguments(getArguments());
        builder.setJvmArguments(getJvmArguments());
        builder.setBlocking(!isNonBlocking());
        return builder.create();
    }

    private static List<String> splitTextIgnoreVars(String text, String delimiters) {
        List<String> result = new LinkedList<>();

        StringBuilder currentPart = new StringBuilder();

        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '$') {
                int nextIndex = i + 1;
                if (nextIndex < text.length()) {
                    char nextCh = text.charAt(nextIndex);
                    if (nextCh == '{') {
                        int varClose = StringUtils.unescapedIndexOf(text, i + 2, '}');
                        if (varClose >= 0) {
                            currentPart.append(text.substring(i, varClose + 1));
                            i = varClose + 1;
                            continue;
                        }
                    }
                }
            }
            else if (delimiters.indexOf(ch) >= 0) {
                result.add(currentPart.toString().trim());
                currentPart.setLength(0);
                i++;
                continue;
            }

            currentPart.append(ch);
            i++;
        }

        result.add(currentPart.toString().trim());

        Iterator<String> resultItr = result.iterator();
        while (resultItr.hasNext()) {
            String value = resultItr.next();
            if (value.isEmpty()) {
                resultItr.remove();
            }
        }

        return result;
    }

    private static List<String> splitBySpacesIgnoreVars(String text) {
        return splitTextIgnoreVars(text, " \t\n\r\f");
    }

    private static List<String> splitLinesIgnoreVars(String text) {
        return splitTextIgnoreVars(text, "\n\r");
    }

    public PredefinedTask tryGetPredefinedTask(String displayName) {
        return tryGetPredefinedTask(displayName, Collections::emptyList);
    }

    public PredefinedTask tryGetPredefinedTask(
            String displayName,
            Supplier<? extends List<PredefinedTask.Name>> fallbackNames) {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(fallbackNames, "fallbackNames");

        boolean tasksMustExist = jMustExistCheck.isSelected();
        List<String> rawTaskNames = getTasks();
        if (rawTaskNames.isEmpty()) {
            return null;
        }

        List<PredefinedTask.Name> names;
        if (!rawTaskNames.isEmpty()) {
            names = new ArrayList<>(rawTaskNames.size());
            for (String name: rawTaskNames) {
                names.add(new PredefinedTask.Name(name, tasksMustExist));
            }
        }
        else {
            names = fallbackNames.get();
        }

        if (names.isEmpty()) {
            return null;
        }

        return new PredefinedTask(
                displayName,
                names,
                getArguments(),
                getJvmArguments(),
                isNonBlocking());
    }

    private List<String> getTasks() {
        String text = jTasksEdit.getText();
        if (text == null) {
            return Collections.emptyList();
        }

        return splitBySpacesIgnoreVars(text);
    }

    private List<String> getArguments() {
        String text = jArgsTextArea.getText();
        if (text == null) {
            return Collections.emptyList();
        }

        return splitLinesIgnoreVars(text);
    }

    private List<String> getJvmArguments() {
        String text = jJvmArgsTextArea.getText();
        if (text == null) {
            return Collections.emptyList();
        }

        return splitLinesIgnoreVars(text);
    }

    public boolean isNonBlocking() {
        return jNonBlockingCheck.isSelected();
    }

    public boolean isTasksMustExist() {
        return jMustExistCheck.isSelected();
    }

    private void traverseWithTab(JTextArea textArea, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_TAB) {
            boolean forward = (event.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0;
            if (forward) {
                textArea.transferFocus();
            }
            else {
                textArea.transferFocusBackward();
            }
            event.consume();
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

        jTasksCaption = new javax.swing.JLabel();
        jTasksEdit = new javax.swing.JTextField();
        jArgsCaption = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jArgsTextArea = new javax.swing.JTextArea();
        jJvmArgsCaption = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jJvmArgsTextArea = new javax.swing.JTextArea();
        jNonBlockingCheck = new javax.swing.JCheckBox();
        jMustExistCheck = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jTasksCaption, org.openide.util.NbBundle.getMessage(CustomActionPanel.class, "CustomActionPanel.jTasksCaption.text")); // NOI18N

        jTasksEdit.setText(org.openide.util.NbBundle.getMessage(CustomActionPanel.class, "CustomActionPanel.jTasksEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jArgsCaption, org.openide.util.NbBundle.getMessage(CustomActionPanel.class, "CustomActionPanel.jArgsCaption.text")); // NOI18N

        jArgsTextArea.setColumns(20);
        jArgsTextArea.setRows(5);
        jArgsTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jArgsTextAreaKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(jArgsTextArea);

        org.openide.awt.Mnemonics.setLocalizedText(jJvmArgsCaption, org.openide.util.NbBundle.getMessage(CustomActionPanel.class, "CustomActionPanel.jJvmArgsCaption.text")); // NOI18N

        jJvmArgsTextArea.setColumns(20);
        jJvmArgsTextArea.setRows(5);
        jJvmArgsTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jJvmArgsTextAreaKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(jJvmArgsTextArea);

        org.openide.awt.Mnemonics.setLocalizedText(jNonBlockingCheck, org.openide.util.NbBundle.getMessage(CustomActionPanel.class, "CustomActionPanel.jNonBlockingCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jMustExistCheck, org.openide.util.NbBundle.getMessage(CustomActionPanel.class, "CustomActionPanel.jMustExistCheck.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTasksEdit)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jNonBlockingCheck)
                                .addGap(18, 18, 18)
                                .addComponent(jMustExistCheck))
                            .addComponent(jTasksCaption)
                            .addComponent(jArgsCaption)
                            .addComponent(jJvmArgsCaption))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTasksCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTasksEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jArgsCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jJvmArgsCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jNonBlockingCheck)
                    .addComponent(jMustExistCheck))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jArgsTextAreaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jArgsTextAreaKeyPressed
        traverseWithTab(jArgsTextArea, evt);
    }//GEN-LAST:event_jArgsTextAreaKeyPressed

    private void jJvmArgsTextAreaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jJvmArgsTextAreaKeyPressed
        traverseWithTab(jJvmArgsTextArea, evt);
    }//GEN-LAST:event_jJvmArgsTextAreaKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jArgsCaption;
    private javax.swing.JTextArea jArgsTextArea;
    private javax.swing.JLabel jJvmArgsCaption;
    private javax.swing.JTextArea jJvmArgsTextArea;
    private javax.swing.JCheckBox jMustExistCheck;
    private javax.swing.JCheckBox jNonBlockingCheck;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel jTasksCaption;
    private javax.swing.JTextField jTasksEdit;
    // End of variables declaration//GEN-END:variables
}
