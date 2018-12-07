package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.ListModel;
import org.jtrim2.property.BoolProperties;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.java.JavaExtensionDef;
import org.netbeans.gradle.project.java.properties.JavaDebuggingPanel;
import org.netbeans.gradle.project.java.properties.JavaModulesPanel;
import org.netbeans.gradle.project.properties.ExtensionActiveSettingsQuery;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.openide.awt.HtmlBrowser;

import static org.jtrim2.property.swing.AutoDisplayState.*;
import static org.netbeans.gradle.project.properties.NbProperties.*;

@SuppressWarnings("serial")
public class GlobalGradleSettingsPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private final PropertySource<CategoryItem> categorySelection;
    private final PropertySource<URL> selectedHelpUrl;

    public GlobalGradleSettingsPanel() {
        initComponents();

        DefaultListModel<CategoryItem> categoriesModel = new DefaultListModel<>();
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryGradleInstallation(),
                GradleInstallationPanel.createSettingsPage()));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryPlatformPriority(),
                PlatformPriorityPanel.createSettingsPage(false)));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryDaemon(),
                GradleDaemonPanel.createSettingsPage()));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryScriptAndTasks(),
                ScriptAndTasksPanel.createSettingsPage()));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryScript(),
                BuildScriptParsingPanel.createSettingsPage()));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryTasks(),
                TaskExecutionPanel.createSettingsPage()));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryJavaModules(),
                JavaModulesPanel.createSettingsPage(false),
                JavaExtensionDef.EXTENSION_NAME));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryDebugJava(),
                JavaDebuggingPanel.createSettingsPage(false),
                JavaExtensionDef.EXTENSION_NAME));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryAppearance(),
                AppearancePanel.createSettingsPage(false)));
        categoriesModel.addElement(new CategoryItem(
                NbStrings.getSettingsCategoryOther(),
                OtherOptionsPanel.createSettingsPage()));

        jCategoriesList.setModel(categoriesModel);
        jCategoriesList.setSelectedIndex(0);

        categorySelection = listSelection(jCategoriesList);
        selectedHelpUrl = PropertyFactory.convert(categorySelection, (CategoryItem input) -> {
            return input != null ? input.getHelpUrl() : null;
        });

        categorySelection.addChangeListener(this::showSelectedEditor);
        showSelectedEditor();

        setupEnableDisable();
    }

    public static GlobalSettingsPage createSettingsPanel() {
        GlobalGradleSettingsPanel panel = new GlobalGradleSettingsPanel();
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(panel);

        result.setValid(panel.valid());

        return result.create();
    }

    private void setupEnableDisable() {
        addSwingStateListener(isNotNull(selectedHelpUrl),
                componentDisabler(jReadWikiButton));
    }

    private void showSelectedEditor() {
        jCurrentCategoryPanel.removeAll();

        CategoryItem selected = categorySelection.getValue();
        if (selected != null) {
            jCurrentCategoryPanel.add(selected.getEditorComponent());
        }

        jCurrentCategoryPanel.revalidate();
        jCurrentCategoryPanel.repaint();
    }

    private StoredSettings combineSettings(
            List<ProfileEditor> editors,
            final Function<? super ProfileEditor, ? extends StoredSettings> settingsGetter) {

        final List<StoredSettings> allSettings = new ArrayList<>(editors.size());
        for (ProfileEditor profileEditor: editors) {
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

    private StoredSettings readCombinedFromSettings(List<ProfileEditor> editors) {
        return combineSettings(editors, ProfileEditor::readFromSettings);
    }

    private StoredSettings readCombinedFromGui(List<ProfileEditor> editors) {
        return combineSettings(editors, ProfileEditor::readFromGui);
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        ListModel<CategoryItem> model = jCategoriesList.getModel();
        int categoryCount = model.getSize();
        final List<ProfileEditor> editors = new ArrayList<>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            CategoryItem item = model.getElementAt(i);
            GlobalSettingsPage editorDef = item.getEditorDef();
            ProfileEditorFactory editorFactory = editorDef.getEditorFactory();
            ProfileEditor editor = editorFactory.startEditingProfile(profileInfo, item.wrapSettings(profileQuery));
            editors.add(editor);
        }

        return new ProfileEditor() {
            @Override
            public StoredSettings readFromSettings() {
                return readCombinedFromSettings(editors);
            }

            @Override
            public StoredSettings readFromGui() {
                return readCombinedFromGui(editors);
            }
        };
    }

    private PropertySource<Boolean> valid() {
        ListModel<CategoryItem> model = jCategoriesList.getModel();
        int categoryCount = model.getSize();

        @SuppressWarnings("unchecked")
        PropertySource<Boolean>[] subValids = (PropertySource<Boolean>[])new PropertySource<?>[categoryCount];
        for (int i = 0; i < categoryCount; i++) {
            subValids[i] = model.getElementAt(i).valid();
        }

        return BoolProperties.or(subValids);
    }

    private static final class CategoryItem {
        private final String caption;
        public final GlobalSettingsPage editorDef;
        private final String extensionName;

        public CategoryItem(String caption, GlobalSettingsPage editorDef) {
            this(caption, editorDef, "");
        }

        public CategoryItem(String caption, GlobalSettingsPage editorDef, String extensionName) {
            this.caption = caption;
            this.editorDef = editorDef;
            this.extensionName = extensionName;
        }

        public GlobalSettingsPage getEditorDef() {
            return editorDef;
        }

        public ActiveSettingsQuery wrapSettings(ActiveSettingsQuery rootSettings) {
            if ("".equals(extensionName)) {
                return rootSettings;
            }
            else {
                return new ExtensionActiveSettingsQuery(rootSettings, extensionName);
            }
        }

        public JComponent getEditorComponent() {
            return editorDef.getSettingsPanel();
        }

        public URL getHelpUrl() {
            return editorDef.getHelpUrl();
        }

        public PropertySource<Boolean> valid() {
            return editorDef.valid();
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
