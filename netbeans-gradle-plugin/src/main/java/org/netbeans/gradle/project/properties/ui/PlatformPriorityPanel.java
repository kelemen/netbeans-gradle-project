package org.netbeans.gradle.project.properties.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.netbeans.gradle.project.properties.standard.PlatformId;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;

import static org.jtrim.property.swing.AutoDisplayState.*;

@SuppressWarnings("serial")
public class PlatformPriorityPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Platform-Priority");

    private final DefaultListModel<PlatformItem> jPlatformListModel;
    private ProfileEditor editor;
    private boolean okPressed;

    private final ListenerRegistrations disableRefs;

    public PlatformPriorityPanel(boolean hasOwnButtons) {
        okPressed = false;
        editor = null;
        disableRefs = new ListenerRegistrations();

        initComponents();

        jOkButton.setVisible(hasOwnButtons);
        jCancelButton.setVisible(hasOwnButtons);

        jPlatformListModel = new DefaultListModel<>();
        jPlatformList.setModel(jPlatformListModel);

        setupEnableDisable();
    }

    public static GlobalSettingsPage createSettingsPage(boolean hasOwnButtons) {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new PlatformPriorityPanel(hasOwnButtons));
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    private void displayPlatformOrder(PlatformOrder platformOrder) {
        JavaPlatform[] platforms
                = JavaPlatformManager.getDefault().getInstalledPlatforms();

        // To increase the model's iternal capacity
        jPlatformListModel.setSize(platforms.length);
        jPlatformListModel.setSize(0);

        List<JavaPlatform> orderedPlatforms = platformOrder.orderPlatforms(Arrays.asList(platforms));
        for (JavaPlatform platform: orderedPlatforms) {
            jPlatformListModel.addElement(new PlatformItem(platform));
        }
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private PlatformOrder getPlatformOrder() {
        List<JavaPlatform> platforms = new ArrayList<>(jPlatformListModel.size());

        Enumeration<PlatformItem> listItems = jPlatformListModel.elements();
        while (listItems.hasMoreElements()) {
            PlatformItem item = listItems.nextElement();
            platforms.add(item.platform);
        }

        return new PlatformOrder(platforms);
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    private void startEditor(String title, ActiveSettingsQuery settingsQuery) {
        ProfileKey key = settingsQuery.currentProfileSettings().getValue().getKey();

        editor = startEditingProfile(
                new ProfileInfo(key, title),
                settingsQuery);
        editor.readFromSettings().displaySettings();
    }

    public static boolean showDialog(Component parent) {
        return showDialog(parent, CommonGlobalSettings.getDefaultActiveSettingsQuery());
    }

    public static boolean showDialog(Component parent, ActiveSettingsQuery settingsQuery) {
        // TODO: I18N
        String title = "Platform order";
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(parentWindow, title, Dialog.ModalityType.DOCUMENT_MODAL);

        Container contentPane = dlg.getContentPane();
        contentPane.setLayout(new GridLayout(1, 1));

        PlatformPriorityPanel content = new PlatformPriorityPanel(true);
        content.startEditor(title, settingsQuery);

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
        disableRefs.unregisterAll();

        PropertySource<Integer> selectedIndex = selectedIndexProperty(jPlatformList);

        disableRefs.add(addSwingStateListener(NbProperties.greaterThanOrEqual(selectedIndex, 1),
                componentDisabler(jMoveUpButton)));

        disableRefs.add(addSwingStateListener(NbProperties.between(selectedIndex, 0, jPlatformListModel.size() - 2),
                componentDisabler(jMoveDownButton)));
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

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<PlatformOrder> platformOrderRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            platformOrderRef = CommonGlobalSettings.platformPreferenceOrder(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, PlatformPriorityPanel.this);
        }
    }

    private final class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final PlatformOrder platformOrder;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.platformOrder = properties.platformOrderRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, PlatformPriorityPanel panel) {
            this.properties = properties;
            this.platformOrder = panel.getPlatformOrder();
        }

        @Override
        public void displaySettings() {
            displayPlatformOrder(platformOrder != null
                    ? platformOrder
                    : properties.platformOrderRef.getActiveValue());

            setupEnableDisable();
        }

        @Override
        public void saveSettings() {
            properties.platformOrderRef.setValue(platformOrder);
        }
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
        if (editor != null) {
            editor.readFromGui().saveSettings();
        }

        okPressed = true;
        closeWindow();
    }//GEN-LAST:event_jOkButtonActionPerformed

    private static final class PlatformItem {
        public final JavaPlatform platform;
        private final String caption;

        public PlatformItem(JavaPlatform platform) {
            this.platform = platform;
            this.caption = PlatformId.getDisplayNameOfPlatform(platform);
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
