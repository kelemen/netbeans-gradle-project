package org.netbeans.gradle.project.properties.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.SwingForwarderFactory;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsEditor;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.netbeans.gradle.project.properties.global.SettingsEditorProperties;
import org.netbeans.gradle.project.util.NbFileUtils;

import static org.jtrim.property.swing.AutoDisplayState.*;

@SuppressWarnings("serial")
public class PlatformPriorityPanel extends javax.swing.JPanel implements GlobalSettingsEditor {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Platform-Priority");

    private final DefaultListModel<PlatformItem> jPlatformListModel;
    private boolean okPressed;

    public PlatformPriorityPanel(boolean hasOwnButtons) {
        okPressed = false;

        initComponents();

        jOkButton.setVisible(hasOwnButtons);
        jCancelButton.setVisible(hasOwnButtons);

        jPlatformListModel = new DefaultListModel<>();
        jPlatformList.setModel(jPlatformListModel);

        setupEnableDisable();
    }

    @Override
    public final void updateSettings(GlobalGradleSettings globalSettings) {
        JavaPlatform[] platforms
                = JavaPlatformManager.getDefault().getInstalledPlatforms();

        // To increase the model's iternal capacity
        jPlatformListModel.setSize(platforms.length);
        jPlatformListModel.setSize(0);

        for (JavaPlatform platform: globalSettings.orderPlatforms(platforms)) {
            jPlatformListModel.addElement(new PlatformItem(platform));
        }

        setupEnableDisable();
    }

    @Override
    public final void saveSettings(GlobalGradleSettings globalSettings) {
        List<JavaPlatform> platforms = new ArrayList<>(jPlatformListModel.size());

        Enumeration<PlatformItem> listItems = jPlatformListModel.elements();
        while (listItems.hasMoreElements()) {
            PlatformItem item = listItems.nextElement();
            platforms.add(item.platform);
        }

        PlatformOrder newOrder = new PlatformOrder(platforms);
        globalSettings.platformPreferenceOrder().setValue(newOrder);
    }

    @Override
    public SettingsEditorProperties getProperties() {
        SettingsEditorProperties.Builder result = new SettingsEditorProperties.Builder(this);
        result.setHelpUrl(HELP_URL);

        return result.create();
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    public static boolean showDialog(Component parent) {
        String title = "Platform order";
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(parentWindow, title, Dialog.ModalityType.DOCUMENT_MODAL);

        Container contentPane = dlg.getContentPane();
        contentPane.setLayout(new GridLayout(1, 1));

        PlatformPriorityPanel content = new PlatformPriorityPanel(true);
        content.updateSettings(GlobalGradleSettings.getDefault());
        contentPane.add(content);

        dlg.pack();
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);

        return content.okPressed;
    }

    private void moveElement(int step) {
        int selectedIndex = jPlatformList.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        PlatformItem item = jPlatformListModel.get(selectedIndex);
        jPlatformListModel.removeElementAt(selectedIndex);

        int newIndex = selectedIndex + step;
        if (newIndex < 0) newIndex = 0;
        else if (newIndex >= jPlatformListModel.size()) newIndex = jPlatformListModel.size();

        jPlatformListModel.add(newIndex, item);
        jPlatformList.setSelectedIndex(newIndex);
    }

    private void setupEnableDisable() {
        PropertySource<Integer> selectedIndex = selectedIndexProperty(jPlatformList);

        addSwingStateListener(NbProperties.greaterThanOrEqual(selectedIndex, 1),
                componentDisabler(jMoveUpButton));

        addSwingStateListener(NbProperties.between(selectedIndex, 0, jPlatformListModel.size() - 2),
                componentDisabler(jMoveDownButton));
    }

    private static org.jtrim.property.PropertySource<Integer> selectedIndexProperty(JList<?> list) {
        final ListSelectionModel selectionModel = list.getSelectionModel();
        SwingPropertySource<Integer, ListSelectionListener> swingSource = new SwingPropertySource<Integer, ListSelectionListener>() {
            @Override
            public Integer getValue() {
                return selectionModel.getMinSelectionIndex();
            }

            @Override
            public void addChangeListener(ListSelectionListener listener) {
                selectionModel.addListSelectionListener(listener);
            }

            @Override
            public void removeChangeListener(ListSelectionListener listener) {
                selectionModel.removeListSelectionListener(listener);
            }
        };
        SwingForwarderFactory<ListSelectionListener> listenerForwarder = new SwingForwarderFactory<ListSelectionListener>() {
            @Override
            public ListSelectionListener createForwarder(final Runnable listener) {
                return new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        listener.run();
                    }
                };
            }
        };
        return SwingProperties.fromSwingSource(swingSource, listenerForwarder);
    }

    private void closeWindow() {
        SwingUtilities.getWindowAncestor(this).dispose();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "Convert2Diamond"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPlatformList = new javax.swing.JList<>();
        jPlatformListTitle = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jMoveUpButton = new javax.swing.JButton();
        jMoveDownButton = new javax.swing.JButton();
        jOkButton = new javax.swing.JButton();
        jCancelButton = new javax.swing.JButton();

        jPlatformList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jPlatformList);

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformListTitle, org.openide.util.NbBundle.getMessage(PlatformPriorityPanel.class, "PlatformPriorityPanel.jPlatformListTitle.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jMoveUpButton, org.openide.util.NbBundle.getMessage(PlatformPriorityPanel.class, "PlatformPriorityPanel.jMoveUpButton.text")); // NOI18N
        jMoveUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMoveUpButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jMoveDownButton, org.openide.util.NbBundle.getMessage(PlatformPriorityPanel.class, "PlatformPriorityPanel.jMoveDownButton.text")); // NOI18N
        jMoveDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMoveDownButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jOkButton, org.openide.util.NbBundle.getMessage(PlatformPriorityPanel.class, "PlatformPriorityPanel.jOkButton.text")); // NOI18N
        jOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOkButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jCancelButton, org.openide.util.NbBundle.getMessage(PlatformPriorityPanel.class, "PlatformPriorityPanel.jCancelButton.text")); // NOI18N
        jCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jMoveUpButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jMoveDownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jOkButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCancelButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jMoveUpButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMoveDownButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 136, Short.MAX_VALUE)
                .addComponent(jOkButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCancelButton))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPlatformListTitle)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPlatformListTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jMoveUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMoveUpButtonActionPerformed
        moveElement(-1);
    }//GEN-LAST:event_jMoveUpButtonActionPerformed

    private void jMoveDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMoveDownButtonActionPerformed
        moveElement(1);
    }//GEN-LAST:event_jMoveDownButtonActionPerformed

    private void jCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCancelButtonActionPerformed
        closeWindow();
    }//GEN-LAST:event_jCancelButtonActionPerformed

    private void jOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jOkButtonActionPerformed
        saveSettings(GlobalGradleSettings.getDefault());

        okPressed = true;
        closeWindow();
    }//GEN-LAST:event_jOkButtonActionPerformed

    private static final class PlatformItem {
        public final JavaPlatform platform;
        private final String caption;

        public PlatformItem(JavaPlatform platform) {
            this.platform = platform;
            this.caption = platform.getDisplayName();
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jCancelButton;
    private javax.swing.JButton jMoveDownButton;
    private javax.swing.JButton jMoveUpButton;
    private javax.swing.JButton jOkButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JList<PlatformItem> jPlatformList;
    private javax.swing.JLabel jPlatformListTitle;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
