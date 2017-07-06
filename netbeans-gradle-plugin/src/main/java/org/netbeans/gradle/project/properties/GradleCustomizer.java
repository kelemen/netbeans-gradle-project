package org.netbeans.gradle.project.properties;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsCategory;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPageFactory;
import org.netbeans.gradle.project.api.entry.GradleProjectIDs;
import org.netbeans.gradle.project.others.ChangeLFPlugin;
import org.netbeans.gradle.project.properties.ui.AppearancePanel;
import org.netbeans.gradle.project.properties.ui.CommonProjectPropertiesPanel;
import org.netbeans.gradle.project.properties.ui.CustomVariablesPanel;
import org.netbeans.gradle.project.properties.ui.LicenseHeaderPanel;
import org.netbeans.gradle.project.properties.ui.ManageBuiltInTasksPanel;
import org.netbeans.gradle.project.properties.ui.ManageTasksPanel;
import org.netbeans.gradle.project.properties.ui.ProfileBasedPanel;
import org.netbeans.modules.editor.indent.project.api.Customizers;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.implspi.NamedServicesProvider;

public final class GradleCustomizer implements CustomizerProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleCustomizer.class.getName());

    private final NbGradleProject project;

    public GradleCustomizer(NbGradleProject project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    private static ProjectCustomizer.CompositeCategoryProvider[] getExternalCustomizers() {
        List<ProjectCustomizer.CompositeCategoryProvider> result = new ArrayList<>();

        result.add(Customizers.createFormattingCategoryProvider(Collections.emptyMap()));

        ProjectCustomizer.CompositeCategoryProvider changeLFProperties = ChangeLFPlugin.getProjectSettings();
        if (changeLFProperties != null) {
            result.add(changeLFProperties);
        }

        return result.toArray(new ProjectCustomizer.CompositeCategoryProvider[result.size()]);
    }

    private static ProfileBasedCustomizer toCustomizer(
            final NbGradleProject project,
            ProfileBasedSettingsCategory settingsCategory) {
        CustomizerCategoryId categoryId = settingsCategory.getCategoryId();
        final ProfileBasedSettingsPageFactory pageFactory = settingsCategory.getSettingsPageFactory();

        return new ProfileBasedCustomizer(categoryId.getCategoryName(), categoryId.getDisplayName(), () -> {
            ProfileBasedSettingsPage settingsPage = pageFactory.createSettingsPage();
            return ProfileBasedPanel.createPanel(project, settingsPage);
        });
    }

    private static ProfileBasedCustomizer newMainCustomizer(NbGradleProject project) {
        return toCustomizer(project, CommonProjectPropertiesPanel.createSettingsCategory(project));
    }

    private static ProfileBasedCustomizer newBuiltInTasksCustomizer(NbGradleProject project) {
        return toCustomizer(project, ManageBuiltInTasksPanel.createSettingsCategory(project));
    }

    private static ProfileBasedCustomizer newCustomTasksCustomizer(NbGradleProject project) {
        return toCustomizer(project, ManageTasksPanel.createSettingsCategory());
    }

    private static ProfileBasedCustomizer newLicenseCustomizer(NbGradleProject project) {
        return toCustomizer(project, LicenseHeaderPanel.createSettingsCategory(project, project.getLicenseSource()));
    }

    private static ProfileBasedCustomizer newAppearanceCustomizer(NbGradleProject project) {
        return toCustomizer(project, AppearancePanel.createSettingsCategory(true));
    }

    private static ProfileBasedCustomizer newCustomVariablesCustomizer(NbGradleProject project) {
        return toCustomizer(project, CustomVariablesPanel.createSettingsCategory());
    }

    private static Collection<? extends ProjectCustomizer.CompositeCategoryProvider> getAnnotationBasedProviders() {
        Lookup customizerLookup = NamedServicesProvider.forPath("Projects/" + GradleProjectIDs.MODULE_NAME + "/Customizer");
        return customizerLookup.lookupAll(ProjectCustomizer.CompositeCategoryProvider.class);
    }

    private void getCustomizersOfExtensions(List<ProjectCustomizer.CompositeCategoryProvider> result) {
        Collection<? extends ExtensionProjectSettingsPageDefs> defs
                = project.getExtensions().lookupAllExtensionObjs(ExtensionProjectSettingsPageDefs.class);
        for (ExtensionProjectSettingsPageDefs def: defs) {
            result.addAll(def.getCustomizers());
        }
    }

    private ProjectCustomizer.CompositeCategoryProvider[] getAllCustomizers() {
        ProjectCustomizer.CompositeCategoryProvider[] externalCategories
                = getExternalCustomizers();
        List<ProjectCustomizer.CompositeCategoryProvider> allCategoriesList
                = new ArrayList<>(externalCategories.length + 2);

        allCategoriesList.add(newMainCustomizer(project));
        allCategoriesList.add(newBuiltInTasksCustomizer(project));
        allCategoriesList.add(newCustomTasksCustomizer(project));
        allCategoriesList.add(newCustomVariablesCustomizer(project));
        allCategoriesList.add(newLicenseCustomizer(project));
        getCustomizersOfExtensions(allCategoriesList);
        allCategoriesList.add(newAppearanceCustomizer(project));
        allCategoriesList.addAll(Arrays.asList(externalCategories));
        allCategoriesList.addAll(getAnnotationBasedProviders());

        return allCategoriesList.toArray(new ProjectCustomizer.CompositeCategoryProvider[allCategoriesList.size()]);
    }

    @Override
    public void showCustomizer() {
        final Lookup lookup = Lookups.fixed(project);

        final ProjectCustomizer.CompositeCategoryProvider[] customizers
                = getAllCustomizers();

        final Map<String, ProjectCustomizer.CompositeCategoryProvider> customizersByName
                = CollectionUtils.newHashMap(customizers.length);

        final ProjectCustomizer.Category[] categories =
                new ProjectCustomizer.Category[customizers.length];
        for (int i = 0; i < categories.length; i++) {
            categories[i] = customizers[i].createCategory(lookup);

            String name = categories[i].getName();
            if (customizersByName.containsKey(name)) {
                LOGGER.log(Level.WARNING, "Customizer with the name already exists: {0}", name);
            }
            else {
                customizersByName.put(name, customizers[i]);
            }
        }

        ProjectCustomizer.CategoryComponentProvider panelProvider = (ProjectCustomizer.Category category) -> {
            String name = category.getName();
            if (name == null) {
                LOGGER.log(Level.WARNING, "null category name.");
                return new JPanel();
            }

            ProjectCustomizer.CompositeCategoryProvider customizer = customizersByName.get(name);
            if (customizer == null) {
                LOGGER.log(Level.WARNING, "Requested category cannot be found {0}.", name);
                return new JPanel();
            }

            return customizer.createComponent(category, lookup);
        };

        Dialog dlg = ProjectCustomizer.createCustomizerDialog(
                categories,
                panelProvider,
                CommonProjectPropertiesPanel.CATEGORY_ID.getCategoryName(),
                e -> { },
                HelpCtx.DEFAULT_HELP);

        dlg.setTitle(NbStrings.getProjectPropertiesDlgTitle(project.getDisplayName()));
        dlg.setModal(true);
        dlg.setVisible(true);
    }

}
