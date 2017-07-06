package org.netbeans.gradle.project.tasks.vars;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.util.StringUtils;

@SuppressWarnings("serial")
public final class TaskVariableQueryDialog extends JDialog {
    private static final Map<String, UserVariableFactory> FACTORY_MAP = variableFactoryMap();
    private static final Map<String, Integer> TYPE_ORDER = typeOrderMap();

    private final List<UserVariable> variablesToQuery;

    private TaskVariableQueryDialog(Collection<DisplayedTaskVariable> variablesToQuery) {
        super((Frame)null, true);

        this.variablesToQuery = toUserVariables(variablesToQuery);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(NbIcons.getGradleIcon());
    }

    private static Map<String, UserVariableFactory> variableFactoryMap() {
        Map<String, UserVariableFactory> result = new HashMap<>();

        result.put(VariableTypeDescription.TYPE_NAME_BOOL, BoolVariable::new);
        result.put(VariableTypeDescription.TYPE_NAME_ENUM, EnumVariable::new);
        result.put(VariableTypeDescription.TYPE_NAME_STRING, StringVariable::new);

        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Integer> typeOrderMap() {
        Map<String, Integer> result = new HashMap<>();

        result.put(VariableTypeDescription.TYPE_NAME_STRING, 0);
        result.put(VariableTypeDescription.TYPE_NAME_ENUM, 1);
        result.put(VariableTypeDescription.TYPE_NAME_BOOL, 2);

        return Collections.unmodifiableMap(result);
    }

    private static int getTypeOrder(DisplayedTaskVariable var) {
        Integer order = TYPE_ORDER.get(var.getTypeDescription().getTypeName());
        return order != null ? order : 0;
    }

    private static List<UserVariable> toUserVariables(Collection<DisplayedTaskVariable> variables) {
        List<UserVariable> result = new ArrayList<>(variables.size());
        for (DisplayedTaskVariable variable: variables) {
            String tpyeName = variable.getTypeDescription().getTypeName();

            UserVariableFactory variableFactory = FACTORY_MAP.get(tpyeName);
            if (variableFactory == null) {
                variableFactory = StringVariable::new;
            }

            result.add(variableFactory.createVariable(variable));
        }

        result.sort((UserVariable o1, UserVariable o2) -> {
            DisplayedTaskVariable var1 = o1.getDisplayedVariable();
            DisplayedTaskVariable var2 = o2.getDisplayedVariable();

            int typeOrder1 = getTypeOrder(var1);
            int typeOrder2 = getTypeOrder(var2);
            if (typeOrder1 != typeOrder2) {
                return typeOrder1 < typeOrder2 ? -1 : 1;
            }

            return StringUtils.STR_CMP.compare(var1.getDisplayName(), var2.getDisplayName());
        });

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

    public static Map<DisplayedTaskVariable, String> queryVariables(
            Collection<DisplayedTaskVariable> variablesToQuery) {
        TaskVariableQueryDialog dlg = new TaskVariableQueryDialog(variablesToQuery);
        return dlg.queryVariables();
    }

    private Map<DisplayedTaskVariable, String> queryVariables() {
        if (variablesToQuery.isEmpty()) {
            return Collections.emptyMap();
        }

        getContentPane().removeAll();

        JPanel queryPanel = createQueryPanel();
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton(NbStrings.getOkOption());
        JButton cancelButton = new JButton(NbStrings.getCancelOption());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        getRootPane().setDefaultButton(okButton);

        final AtomicBoolean okPressed = new AtomicBoolean(false);
        okButton.addActionListener(e -> {
            okPressed.set(true);
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());

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

        if (!okPressed.get()) {
            return null;
        }

        Map<DisplayedTaskVariable, String> result = new HashMap<>();
        for (UserVariable variable: variablesToQuery) {
            result.put(variable.getDisplayedVariable(), variable.getValue());
        }
        return result;
    }

    private static final class BoolVariable implements UserVariable {
        private final DisplayedTaskVariable variable;
        private final JCheckBox checkBox;
        private final String[] possibleValues;

        public BoolVariable(DisplayedTaskVariable variable) {
            this.variable = Objects.requireNonNull(variable, "variable");
            this.checkBox = new JCheckBox(variable.getDisplayName());

            String typeArguments = variable.getTypeDescription().getEscapedTypeArguments();
            this.possibleValues = parsePossibleValues(typeArguments, checkBox);
        }

        private static String[] parsePossibleValues(String valuesStr, JCheckBox checkbox) {
            String[] values = StringUtils.unescapedSplit(valuesStr, ',', 2);
            if (values.length < 2) {
                return new String[]{Boolean.FALSE.toString(), Boolean.TRUE.toString()};
            }

            for (int i = 0; i < 2; i++) {
                values[i] = values[i].trim();
                if (values[i].startsWith("*")) {
                    values[i] = values[i].substring(1).trim();
                    checkbox.setSelected(i != 0);
                }
                values[i] = StringUtils.unescapeString(values[i]);
            }
            return values;
        }

        @Override
        public DisplayedTaskVariable getDisplayedVariable() {
            return variable;
        }

        @Override
        public void addToSequential(SequentialGroup group) {
            group.addComponent(checkBox);
        }

        @Override
        public void addToParallel(ParallelGroup group) {
            group.addComponent(checkBox);
        }

        @Override
        public String getValue() {
            return checkBox.isSelected()
                    ? possibleValues[1]
                    : possibleValues[0];
        }
    }

    private static final class EnumVariable implements UserVariable {
        private final DisplayedTaskVariable variable;
        private final JLabel label;
        private final JComboBox<ComboValue> value;

        public EnumVariable(DisplayedTaskVariable variable) {
            this.variable = Objects.requireNonNull(variable, "variable");
            this.label = new JLabel(variable.getDisplayName());
            this.value = new JComboBox<>();

            parseComboValues(variable.getTypeDescription().getEscapedTypeArguments(), value);
        }

        private static void parseComboValues(String valuesDef, JComboBox<ComboValue> combo) {
            String[] values = StringUtils.unescapedSplit(valuesDef, ',');

            ComboValue[] comboValues = new ComboValue[values.length];
            int selectedIndex = 0;
            for (int i = 0; i < values.length; i++) {
                String valueDef = values[i].trim();
                if (valueDef.startsWith("*")) {
                    valueDef = valueDef.substring(1).trim();
                    selectedIndex = i;
                }

                comboValues[i] = parseComboValue(valueDef);
            }

            ComboValue selected = comboValues[selectedIndex];
            Arrays.sort(comboValues, (o1, o2) -> {
                return StringUtils.STR_CMP.compare(o1.displayValue, o2.displayValue);
            });

            combo.setModel(new DefaultComboBoxModel<>(comboValues));
            combo.setSelectedItem(selected);
        }

        private static ComboValue parseComboValue(String valueDef) {
            String[] valueAndDisplay = StringUtils.unescapedSplit(valueDef, ':', 2);
            for (int i = 0; i < valueAndDisplay.length; i++) {
                valueAndDisplay[i] = StringUtils.unescapeString(valueAndDisplay[i]).trim();
            }

            String value = valueAndDisplay[0];
            String displayValue = valueAndDisplay.length < 2
                    ? value
                    : valueAndDisplay[1];
            return new ComboValue(value, displayValue);
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
            ComboValue selected = (ComboValue)value.getSelectedItem();
            return selected != null ? selected.value : "";
        }
    }

    private static final class StringVariable implements UserVariable {
        private final DisplayedTaskVariable variable;
        private final JLabel label;
        private final JTextField value;

        public StringVariable(DisplayedTaskVariable variable) {
            this.variable = Objects.requireNonNull(variable, "variable");
            this.label = new JLabel(variable.getDisplayName());

            String typeArguments = variable.getTypeDescription().getEscapedTypeArguments();
            this.value = new JTextField(StringUtils.unescapeString(typeArguments));
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

    private interface UserVariableFactory {
        public UserVariable createVariable(DisplayedTaskVariable variable);
    }

    private static final class ComboValue {
        public final String value;
        public final String displayValue;

        public ComboValue(String value, String displayValue) {
            this.value = value;
            this.displayValue = displayValue;
        }

        @Override
        public String toString() {
            return displayValue;
        }
    }
}
