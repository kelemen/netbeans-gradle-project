package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.LazyValues;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedForeignSettingsCategory;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsCategory;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPageFactory;
import org.netbeans.gradle.project.properties.ui.ProfileBasedPanel;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;

public final class ExtensionProjectSettingsPageDefs {
    private final NbGradleProject project;
    private final Lookup[] extensionLookups;

    // Lazily initialize to avoid too early project access.
    private final Supplier<ProjectSettingsProvider.ExtensionSettings> extensionSettingsRef;

    public ExtensionProjectSettingsPageDefs(
            NbGradleProject project,
            String extensionName,
            Lookup... extensionLookups) {
        this.project = Objects.requireNonNull(project, "project");
        this.extensionLookups = extensionLookups.clone();

        Objects.requireNonNull(extensionName, "extensionName");
        this.extensionSettingsRef = LazyValues.lazyValue(() -> {
            ProjectSettingsProvider projectSettingsProvider = project.getProjectSettingsProvider();
            return projectSettingsProvider.getExtensionSettings(extensionName);
        });

        ExceptionHelper.checkNotNullElements(this.extensionLookups, "extensionLookups");
    }

    public static ProjectCustomizer.CompositeCategoryProvider createProfileBasedCustomizer(
            @Nonnull final NbGradleProject project,
            @Nonnull final CustomizerCategoryId categoryId,
            @Nonnull final ProjectSettingsProvider.ExtensionSettings extensionSettings,
            @Nonnull final ProfileBasedSettingsPageFactory pageFactory) {

        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(extensionSettings, "extensionSettings");
        Objects.requireNonNull(pageFactory, "pageFactory");

        return new ProfileBasedCustomizer(categoryId.getCategoryName(), categoryId.getDisplayName(), () -> {
            ProfileBasedSettingsPage settingsPage = pageFactory.createSettingsPage();
            return ProfileBasedPanel.createPanel(project, extensionSettings, settingsPage);
        });
    }

    private ProjectSettingsProvider.ExtensionSettings getExtensionSettings() {
        return extensionSettingsRef.get();
    }

    public void addAllExplicitProviders(List<ProjectCustomizer.CompositeCategoryProvider> result) {
        for (Lookup lookup: extensionLookups) {
            for (ProjectCustomizer.CompositeCategoryProvider provider: lookup.lookupAll(ProjectCustomizer.CompositeCategoryProvider.class)) {
                result.add(provider);
            }
        }
    }

    public void addAllGenericProviders(List<ProjectCustomizer.CompositeCategoryProvider> result) {
        ProjectSettingsProvider.ExtensionSettings extSettings = getExtensionSettings();

        for (Lookup lookup: extensionLookups) {
            for (ProfileBasedSettingsCategory categoryDef: lookup.lookupAll(ProfileBasedSettingsCategory.class)) {
                CustomizerCategoryId categoryId = categoryDef.getCategoryId();
                ProfileBasedSettingsPageFactory pageFactory = categoryDef.getSettingsPageFactory();

                result.add(createProfileBasedCustomizer(project, categoryId, extSettings, pageFactory));
            }
        }
    }

    public void addAllForeignProviders(List<ProjectCustomizer.CompositeCategoryProvider> result) {
        ProjectSettingsProvider settingsProvider = project.getProjectSettingsProvider();

        for (Lookup lookup: extensionLookups) {
            for (ProfileBasedForeignSettingsCategory foreignDef: lookup.lookupAll(ProfileBasedForeignSettingsCategory.class)) {
                ProfileBasedSettingsCategory categoryDef = foreignDef.getSettingsCategory();

                CustomizerCategoryId categoryId = categoryDef.getCategoryId();
                ProfileBasedSettingsPageFactory pageFactory = categoryDef.getSettingsPageFactory();

                ProjectSettingsProvider.ExtensionSettings extSettings
                        = settingsProvider.getExtensionSettings(foreignDef.getExtensionName());

                result.add(createProfileBasedCustomizer(project, categoryId, extSettings, pageFactory));
            }
        }
    }

    public List<ProjectCustomizer.CompositeCategoryProvider> getCustomizers() {
        List<ProjectCustomizer.CompositeCategoryProvider> result = new ArrayList<>();
        addAllGenericProviders(result);
        addAllExplicitProviders(result);
        addAllForeignProviders(result);
        return result;
    }
}
