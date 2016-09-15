package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import org.jtrim.property.BoolProperties;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.properties.ProfileEditor;
import org.netbeans.gradle.project.properties.ProfileInfo;
import org.netbeans.gradle.project.properties.StoredSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsEditor;
import org.netbeans.gradle.project.properties.global.SettingsEditorProperties;
import org.netbeans.gradle.project.util.NbFunction;
import org.openide.awt.HtmlBrowser;

import static org.jtrim.property.swing.AutoDisplayState.*;
import static org.netbeans.gradle.project.properties.NbProperties.*;

@SuppressWarnings("serial")
public class GlobalGradleSettingsPanel extends javax.swing.JPanel implements GlobalSettingsEditor {
    private final PropertySource<CategoryItem> categorySelection;
    private final PropertySource<URL> selectedHelpUrl;

    public GlobalGradleSettingsPanel(ActiveSettingsQuery settingsQuery) {
        initComponents();

        DefaultListModel<CategoryItem> categoriesModel = new DefaultListModel<>();
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryGradleInstallation(),
                new GradleInstallationPanel(),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryPlatformPriority(),
                new PlatformPriorityPanel(false),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryDaemon(),
                new GradleDaemonPanel(),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryScriptAndTasks(),
                new ScriptAndTasksPanel(),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryScript(),
                new BuildScriptParsingPanel(),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryTasks(),
                new TaskExecutionPanel(),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryDebug(),
                new DebuggerPanel(),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryAppearance(),
                new AppearancePanel(),
                settingsQuery));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryOther(),
                new OtherOptionsPanel(),
                settingsQuery));

        jCategoriesList.setModel(categoriesModel);
        jCategoriesList.setSelectedIndex(0);

        categorySelection = listSelection(jCategoriesList);
        selectedHelpUrl = PropertyFactory.convert(categorySelection, new ValueConverter<CategoryItem, URL>() {
            @Override
            public URL convert(CategoryItem input) {
                return input != null ? input.properties.getHelpUrl() : null;
            }
        });

        categorySelection.addChangeListener(new Runnable() {
            @Override
            public void run() {
                showSelectedEditor();
            }
        });
        showSelectedEditor();

        setupEnableDisable();
    }

    private void setupEnableDisable() {
        addSwingStateListener(isNotNull(selectedHelpUrl),
                componentDisabler(jReadWikiButton));
    }

    private void showSelectedEditor() {
        jCurrentCategoryPanel.removeAll();

        CategoryItem selected = categorySelection.getValue();
        if (selected != null) {
            SettingsEditorProperties properties = selected.properties;

            jCurrentCategoryPanel.add(properties.getEditorComponent());
        }

        jCurrentCategoryPanel.revalidate();
        jCurrentCategoryPanel.repaint();
    }

    private StoredSettings combineSettings(
            final NbFunction<? super ProfileEditor, ? extends StoredSettings> settingsGetter) {

        ListModel<CategoryItem> model = jCategoriesList.getModel();
        int categoryCount = model.getSize();
        final List<StoredSettings> allSettings = new ArrayList<>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            ProfileEditor profileEditor = model.getElementAt(i).profileEditor;
            StoredSettings settings = settingsGetter.apply(profileEditor);
            allSettings.add(settings);
        }

        return new StoredSettings() {
            @Override
            public void displaySettings() {
                for (StoredSettings settings: allSettings) {
                    settings.displaySettings();
                }
            }

            @Override
            public void saveSettings() {
                for (StoredSettings settings: allSettings) {
                    settings.saveSettings();
                }
            }
        };
    }

    private StoredSettings readCombinedFromSettings() {
        return combineSettings(new NbFunction<ProfileEditor, StoredSettings>() {
            @Override
            public StoredSettings apply(ProfileEditor editor) {
                return editor.readFromSettings();
            }
        });
    }

    private StoredSettings readCombinedFromGui() {
        return combineSettings(new NbFunction<ProfileEditor, StoredSettings>() {
            @Override
            public StoredSettings apply(ProfileEditor editor) {
                return editor.readFromGui();
            }
        });
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new ProfileEditor() {
            @Override
            public StoredSettings readFromSettings() {
                return readCombinedFromSettings();
            }

            @Override
            public StoredSettings readFromGui() {
                return readCombinedFromGui();
            }
        };
    }

    @Override
    public SettingsEditorProperties getProperties() {
        SettingsEditorProperties.Builder result = new SettingsEditorProperties.Builder(this);
        result.setValid(valid());

        return result.create();
    }

    private PropertySource<Boolean> valid() {
        ListModel<CategoryItem> model = jCategoriesList.getModel();
        int categoryCount = model.getSize();

        @SuppressWarnings("unchecked")
        PropertySource<Boolean>[] subValids = (PropertySource<Boolean>[])new PropertySource<?>[categoryCount];
        for (int i = 0; i < categoryCount; i++) {
            subValids[i] = model.getElementAt(i).properties.valid();
        }

        return BoolProperties.or(subValids);
    }

    private static final class CategoryItem {
        private final String caption;
        public final GlobalSettingsEditor editor;
        public final ProfileEditor profileEditor;
        public final SettingsEditorProperties properties;

        public CategoryItem(String caption, GlobalSettingsEditor editor, ActiveSettingsQuery settingsQuery) {
            this.caption = caption;
            this.editor = editor;

            ProfileInfo info = new ProfileInfo(ProfileKey.GLOBAL_PROFILE, caption);
            this.profileEditor = editor.startEditingProfile(info, settingsQuery);

            this.properties = editor.getProperties();
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCategoriesLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jCategoriesList = new javax.swing.JList<>();
        jCurrentCategoryPanel = new javax.swing.JPanel();
        jReadWikiButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jCategoriesLabel, org.openide.util.NbBundle.getMessage(GlobalGradleSettingsPanel.class, "GlobalGradleSettingsPanel.jCategoriesLabel.text")); // NOI18N

        jScrollPane1.setViewportView(jCategoriesList);

        jCurrentCategoryPanel.setLayout(new java.awt.GridLayout(1, 1));

        org.openide.awt.Mnemonics.setLocalizedText(jReadWikiButton, org.openide.util.NbBundle.getMessage(GlobalGradleSettingsPanel.class, "GlobalGradleSettingsPanel.jReadWikiButton.text")); // NOI18N
        jReadWikiButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jReadWikiButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCurrentCategoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jReadWikiButton)
                                .addGap(0, 72, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jCategoriesLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCategoriesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jReadWikiButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCurrentCategoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jReadWikiButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jReadWikiButtonActionPerformed
        URL helpUrl = selectedHelpUrl.getValue();
        if (helpUrl != null) {
            HtmlBrowser.URLDisplayer.getDefault().showURLExternal(helpUrl);
        }
    }//GEN-LAST:event_jReadWikiButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jCategoriesLabel;
    private javax.swing.JList<CategoryItem> jCategoriesList;
    private javax.swing.JPanel jCurrentCategoryPanel;
    private javax.swing.JButton jReadWikiButton;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
