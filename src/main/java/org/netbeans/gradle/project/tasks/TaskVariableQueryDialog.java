package org.netbeans.gradle.project.tasks;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
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
import org.netbeans.gradle.project.NbStrings;

@SuppressWarnings("serial")
public final class TaskVariableQueryDialog extends JDialog {
    private final List<UserVariable> variablesToQuery;

    public TaskVariableQueryDialog(Collection<DisplayedTaskVariable> variablesToQuery) {
        super((Frame)null, true);

        this.variablesToQuery = toUserVariables(variablesToQuery);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private static List<UserVariable> toUserVariables(Collection<DisplayedTaskVariable> variables) {
        List<UserVariable> result = new ArrayList<UserVariable>(variables.size());
        for (DisplayedTaskVariable variable: variables) {
            result.add(new StringVariable(variable));
        }
        return result;
    }

    private ParallelGroup createParallelGroup(GroupLayout layout) {
        ParallelGroup group = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        for (UserVariable variable: variablesToQuery) {
            variable.addToParallel(group);
        }
        return group;
    }

    private SequentialGroup createSequentialGroup(GroupLayout layout) {
        SequentialGroup group = layout.createSequentialGroup();
        group.addContainerGap();

        boolean first = true;
        for (UserVariable variable: variablesToQuery) {
            if (!first) {
                group.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED);
            }
            first = false;

            variable.addToSequential(group);
        }
        group.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        return group;
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel();

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(createParallelGroup(layout))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(createSequentialGroup(layout))
        );
        return panel;
    }

    public Map<DisplayedTaskVariable, String> queryVariables() {
        if (variablesToQuery.isEmpty()) {
            return Collections.emptyMap();
        }

        getContentPane().removeAll();

        JPanel queryPanel = createQueryPanel();
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton(NbStrings.getOkOption());
        buttonPanel.add(okButton);
        getRootPane().setDefaultButton(okButton);

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

        setTitle(NbStrings.getTaskVariableQueryCaption());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        Map<DisplayedTaskVariable, String> result = new HashMap<DisplayedTaskVariable, String>();
        for (UserVariable variable: variablesToQuery) {
            result.put(variable.getDisplayedVariable(), variable.getValue());
        }
        return result;
    }

    private static final class StringVariable implements UserVariable {
        private final DisplayedTaskVariable variable;
        private final JLabel label;
        private final JTextField value;

        public StringVariable(DisplayedTaskVariable variable) {
            if (variable == null) throw new NullPointerException("variable");

            this.variable = variable;
            this.label = new JLabel(variable.getDisplayName());
            this.value = new JTextField(variable.getTypeDescription().getTypeArguments());
        }

        @Override
        public DisplayedTaskVariable getDisplayedVariable() {
            return variable;
        }

        @Override
        public void addToSequential(SequentialGroup group) {
            group.addComponent(label);
            group.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
            group.addComponent(value, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
        }

        @Override
        public void addToParallel(ParallelGroup group) {
            group.addComponent(label);
            group.addComponent(value);
        }

        @Override
        public String getValue() {
            return value.getText().trim();
        }
    }

    private interface UserVariable {
        public DisplayedTaskVariable getDisplayedVariable();
        public void addToSequential(SequentialGroup group);
        public void addToParallel(ParallelGroup group);
        public String getValue();
    }
}
