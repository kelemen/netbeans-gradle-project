package org.netbeans.gradle.project.tasks;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

@SuppressWarnings("serial")
public final class TaskVariableQueryDialog extends JDialog {
    private final List<DisplayedTaskVariable> variablesToQuery;

    public TaskVariableQueryDialog(Collection<DisplayedTaskVariable> variablesToQuery) {
        super((Frame)null, true);

        this.variablesToQuery = new ArrayList<DisplayedTaskVariable>(variablesToQuery);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private static ParallelGroup createParallelGroup(GroupLayout layout, Component[] components) {
        ParallelGroup group = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        for (Component component: components) {
            group.addComponent(component);
        }
        return group;
    }

    private static SequentialGroup createSequentialGroup(GroupLayout layout, JLabel[] labels, JTextField[] textFields) {
        assert labels.length == textFields.length;

        SequentialGroup group = layout.createSequentialGroup();
        group.addContainerGap();
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                group.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED);
            }

            group.addComponent(labels[i]);
            group.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
            group.addComponent(textFields[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
        }
        group.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        return group;
    }

    private static JPanel createQueryPanel(JLabel[] labels, JTextField[] textFields) {
        JPanel panel = new JPanel();

        Component[] allComponents = new Component[labels.length + textFields.length];
        System.arraycopy(textFields, 0, allComponents, 0, textFields.length);
        System.arraycopy(labels, 0, allComponents, textFields.length, labels.length);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(createParallelGroup(layout, allComponents))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(createSequentialGroup(layout, labels, textFields))
        );
        return panel;
    }

    public Map<DisplayedTaskVariable, String> queryVariables() {
        getContentPane().removeAll();

        int varCount = variablesToQuery.size();

        JLabel[] labels = new JLabel[varCount];
        JTextField[] textFields = new JTextField[varCount];
        for (int i = 0; i < varCount; i++) {
            labels[i] = new JLabel(variablesToQuery.get(i).getDisplayName());
            textFields[i] = new JTextField("");
        }

        JPanel queryPanel = createQueryPanel(labels, textFields);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        // TODO: localize
        JButton okButton = new JButton("Ok");
        buttonPanel.add(okButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(queryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE))
        );

        // TODO: localize
        setTitle("Enter values for variables");
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        Map<DisplayedTaskVariable, String> result = new HashMap<DisplayedTaskVariable, String>();
        for (int i = 0; i < textFields.length; i++) {
            result.put(variablesToQuery.get(i), textFields[i].getText().trim());
        }
        return result;
    }
}
