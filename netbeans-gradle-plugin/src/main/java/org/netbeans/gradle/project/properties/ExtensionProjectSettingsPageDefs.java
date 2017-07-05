package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;
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
    private final String extensionName;
    private final Lookup[] extensionLookups;

    // Lazily initialize to avoid too early project access.
    private final AtomicReference<ProjectSettingsProvider.ExtensionSettings> extensionSettingsRef;

    public ExtensionProjectSettingsPageDefs(
            NbGradleProject project,
            String extensionName,
            Lookup... extensionLookups) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(extensionName, "extensionName");

        this.project = project;
        this.extensionName = extensionName;
        this.extensionLookups = extensionLookups.clone();
        this.extensionSettingsRef = new AtomicReference<>(null);

        ExceptionHelper.checkNotNullElements(this.extensionLookups, "extensionLookups");
    }

    public static ProjectCustomizer.CompositeCategoryProvider createProfileBasedCustomizer(
            @Nonnull final NbGradleProject project,
            @Nonnull final CustomizerCategoryId categoryId,
            @Nonnull final ProjectSettingsProvider.ExtensionSettings extensionSettings,
            @Nonnull final ProfileBasedSettingsPageFactory pageFactory) {

        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(categoryId, "categoryId");
        ExceptionHelper.checkNotNullArgument(extensionSettings, "extensionSettings");
        ExceptionHelper.checkNotNullArgument(pageFactory, "pageFactory");

        return new ProfileBasedCustomizer(categoryId.getCategoryName(), categoryId.getDisplayName(), () -> {
            ProfileBasedSettingsPage settingsPage = pageFactory.createSettingsPage();
            return ProfileBasedPanel.createPanel(project, extensionSettings, settingsPage);
        });
    }

    private ProjectSettingsProvider.ExtensionSettings getExtensionSettings() {
        ProjectSettingsProvider.ExtensionSettings result = extensionSettingsRef.get();
        if (result == null) {
            ProjectSettingsProvider projectSettingsProvider = project.getProjectSettingsProvider();
            result = projectSettingsProvider.getExtensionSettings(extensionName);
            if (!extensionSettingsRef.compareAndSet(null, result)) {
                result = extensionSettingsRef.get();
            }
        }
        return result;
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
