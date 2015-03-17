package org.netbeans.gradle.project.properties;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.others.ChangeLFPlugin;
import org.netbeans.modules.editor.indent.project.api.Customizers;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GradleCustomizer implements CustomizerProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleCustomizer.class.getName());

    private static final String GRADLE_CATEGORY_NAME = GradleCustomizer.class.getName() + ".gradle";
    private static final String BUILT_IN_TASKS_CATEGORY_NAME = GradleCustomizer.class.getName() + ".gradle-built-in-commands";
    private static final String CUSTOM_TASKS_CATEGORY_NAME = GradleCustomizer.class.getName() + ".gradle-custom-tasks";
    private static final String LICENSE_CATEGORY_NAME = GradleCustomizer.class.getName() + ".gradle-license";

    private final NbGradleProject project;

    public GradleCustomizer(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
    }

    private static ProjectCustomizer.CompositeCategoryProvider[] getExternalCustomizers() {
        List<ProjectCustomizer.CompositeCategoryProvider> result
                = new LinkedList<>();

        result.add(Customizers.createFormattingCategoryProvider(Collections.emptyMap()));

        ProjectCustomizer.CompositeCategoryProvider changeLFProperties = ChangeLFPlugin.getProjectSettings();
        if (changeLFProperties != null) {
            result.add(changeLFProperties);
        }

        return result.toArray(new ProjectCustomizer.CompositeCategoryProvider[result.size()]);
    }

    private ProjectCustomizer.CompositeCategoryProvider[] getAllCustomizers() {
        ProjectCustomizer.CompositeCategoryProvider[] externalCategories
                = getExternalCustomizers();
        List<ProjectCustomizer.CompositeCategoryProvider> allCategoriesList
                = new ArrayList<>(externalCategories.length + 2);

        allCategoriesList.add(new MainCustomizer(project));
        allCategoriesList.add(new BuiltInTasksCustomizer(project));
        allCategoriesList.add(new CustomTasksCustomizer(project));
        allCategoriesList.add(new LicenseCustomizer(project));
        allCategoriesList.addAll(project
                .getCombinedExtensionLookup()
                .lookupAll(ProjectCustomizer.CompositeCategoryProvider.class));
        allCategoriesList.addAll(Arrays.asList(externalCategories));

        return allCategoriesList.toArray(new ProjectCustomizer.CompositeCategoryProvider[allCategoriesList.size()]);
    }

    @Override
    public void showCustomizer() {
        final Lookup lookup = Lookups.fixed(project);

        final ProjectCustomizer.CompositeCategoryProvider[] customizers
                = getAllCustomizers();

        final ProjectCustomizer.Category[] categories =
                new ProjectCustomizer.Category[customizers.length];
        for (int i = 0; i < categories.length; i++) {
            categories[i] = customizers[i].createCategory(lookup);
        }

        ProjectCustomizer.CategoryComponentProvider panelProvider = new ProjectCustomizer.CategoryComponentProvider() {
            @Override
            public JComponent create(ProjectCustomizer.Category category) {
                String name = category.getName();
                if (name == null) {
                    LOGGER.log(Level.WARNING, "null category name.");
                    return new JPanel();
                }

                for (int i = 0; i < categories.length; i++) {
                    if (name.equals(categories[i].getName())) {
                        return customizers[i].createComponent(category, lookup);
                    }
                }

                LOGGER.log(Level.WARNING, "Requested category cannot be found {0}.", name);
                return new JPanel();
            }
        };

        ActionListener okListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // no-op
            }
        };

        Dialog dlg = ProjectCustomizer.createCustomizerDialog(
                categories,
                panelProvider,
                GRADLE_CATEGORY_NAME,
                okListener,
                HelpCtx.DEFAULT_HELP);

        dlg.setTitle(NbStrings.getProjectPropertiesDlgTitle(project.getDisplayName()));
        dlg.setModal(true);
        dlg.setVisible(true);
    }

    private static final class MainCustomizer
    implements
            ProjectCustomizer.CompositeCategoryProvider {

        private final ProfileBasedPanel panel;

        public MainCustomizer(NbGradleProject project) {
            this.panel = CommonProjectPropertiesPanel.createProfileBasedPanel(project);
        }

        @Override
        public ProjectCustomizer.Category createCategory(Lookup context) {
            return ProjectCustomizer.Category.create(
                    GRADLE_CATEGORY_NAME,
                    NbStrings.getGradleProjectCategoryName(),
                    null);
        }

        @Override
        public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
            category.setOkButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.saveProperties();
                }
            });
            return panel;
        }
    }

    private static final class BuiltInTasksCustomizer
    implements
            ProjectCustomizer.CompositeCategoryProvider {

        private final ProfileBasedPanel panel;

        public BuiltInTasksCustomizer(NbGradleProject project) {
            this.panel = ManageBuiltInTasksPanel.createProfileBasedPanel(project);
        }

        @Override
        public ProjectCustomizer.Category createCategory(Lookup context) {
            return ProjectCustomizer.Category.create(
                    BUILT_IN_TASKS_CATEGORY_NAME,
                    NbStrings.getManageBuiltInTasksTitle(),
                    null);
        }

        @Override
        public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
            category.setOkButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.saveProperties();
                }
            });
            return panel;
        }
    }

    private static final class CustomTasksCustomizer
    implements
            ProjectCustomizer.CompositeCategoryProvider {

        private final ProfileBasedPanel panel;

        public CustomTasksCustomizer(NbGradleProject project) {
            this.panel = ManageTasksPanel.createProfileBasedPanel(project);
        }

        @Override
        public ProjectCustomizer.Category createCategory(Lookup context) {
            return ProjectCustomizer.Category.create(
                    CUSTOM_TASKS_CATEGORY_NAME,
                    NbStrings.getManageCustomTasksTitle(),
                    null);
        }

        @Override
        public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
            category.setOkButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.saveProperties();
                }
            });
            return panel;
        }
    }

    private static final class LicenseCustomizer
    implements
            ProjectCustomizer.CompositeCategoryProvider {

        private final ProfileBasedPanel panel;

        public LicenseCustomizer(NbGradleProject project) {
            this.panel = LicenseHeaderPanel.createProfileBasedPanel(project);
        }

        @Override
        public ProjectCustomizer.Category createCategory(Lookup context) {
            return ProjectCustomizer.Category.create(
                    LICENSE_CATEGORY_NAME,
                    NbStrings.getGradleProjectLicenseCategoryName(),
                    null);
        }

        @Override
        public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
            category.setOkButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    panel.saveProperties();
                }
            });
            return panel;
        }
    }
}
