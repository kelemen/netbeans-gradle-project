package org.netbeans.gradle.project.java.nodes;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.GradleActionType;
import org.netbeans.gradle.project.api.nodes.GradleProjectAction;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.coverage.CoveragePopup;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.test.TestTaskName;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;

public final class JavaProjectContextActions implements GradleProjectContextActions {
    private final JavaExtension javaExt;

    public JavaProjectContextActions(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    @Override
    public List<Action> getContextActions() {
        List<Action> result = new ArrayList<>();

        CustomTestsAction customTestsAction = new CustomTestsAction();
        if (customTestsAction.hasCustomTestActions()) {
            result.add(customTestsAction);
        }
        result.add(createJavaDocAction());
        result.add(sourcesDirsAction());
        result.add(new CoveragePopup());
        return result;
    }

    private Action createJavaDocAction() {
        return createProjectAction(
                JavaProjectConstants.COMMAND_JAVADOC,
                NbStrings.getJavadocCommandCaption());
    }

    private Action sourcesDirsAction() {
        return new SourceDirsAction();
    }

    private String getCustomTestTaskName(JavaTestTask testTask) {
        return StringUtils.capitalizeFirstCharacter(testTask.getName());
    }

    private static Action backgroundTaskAction(String name, final Runnable action) {
        return new AbstractAction(name) {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        };
    }

    private static JMenuItem backgroundTaskMenuItem(String name, Runnable action) {
        return new JMenuItem(backgroundTaskAction(name, action));
    }

    private final class CustomTestAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private final String displayName;
        private final Lookup context;

        public CustomTestAction(JavaTestTask testTask) {
            this(testTask, getCustomTestTaskName(testTask));
        }

        private CustomTestAction(JavaTestTask testTask, String name) {
            super(name);

            this.context = Lookups.singleton(new TestTaskName(testTask.getName()));
            this.displayName = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GradleActionProvider.invokeAction(javaExt.getProject(), ActionProvider.COMMAND_TEST, context);
        }
    }

    @SuppressWarnings("serial")
    private abstract class SubmenuParentAction extends AbstractAction implements Presenter.Popup {
        private JMenu cachedMenu;

        public SubmenuParentAction() {
            this.cachedMenu = null;
        }

        @Override
        public final JMenuItem getPopupPresenter() {
            if (cachedMenu == null) {
                cachedMenu = createMenu();
            }
            return cachedMenu;
        }

        protected abstract JMenu createMenu();

        @Override
        public final void actionPerformed(ActionEvent e) {
        }
    }

    @SuppressWarnings("serial") // don't care about serialization
    @GradleProjectAction(GradleActionType.PROJECT_MANAGEMENT_ACTION)
    private class SourceDirsAction extends SubmenuParentAction {
        @Override
        protected JMenu createMenu() {
            JMenu menu = new JMenu(NbStrings.getSourceDirsActionGroup());
            menu.add(backgroundTaskMenuItem(NbStrings.getCreateSourceDirsAction(), () -> {
                javaExt.getSourceDirsHandler().createDirectories();
            }));
            menu.add(backgroundTaskMenuItem(NbStrings.getDeleteEmptySourceDirsAction(), () -> {
                javaExt.getSourceDirsHandler().deleteEmptyDirectories();
            }));
            return menu;
        }
    }

    private static void sortTestActions(List<CustomTestAction> actions) {
        Collections.sort(actions, Comparator.comparing(CustomTestAction::getDisplayName, StringUtils.STR_CMP::compare));
    }

    @SuppressWarnings("serial") // don't care about serialization
    private class CustomTestsAction extends SubmenuParentAction {
        private final List<CustomTestAction> customTestTasks;

        public CustomTestsAction() {
            JavaTestModel testTasksModel = javaExt.getCurrentModel().getMainModule().getTestTasks();
            Collection<JavaTestTask> testTasks = testTasksModel.getTestTasks();
            this.customTestTasks = new ArrayList<>(testTasks.size());

            for (JavaTestTask testTask: testTasks) {
                if (!TestTaskName.DEFAULT_TEST_TASK_NAME.equals(testTask.getName())) {
                    customTestTasks.add(new CustomTestAction(testTask));
                }
            }

            sortTestActions(customTestTasks);
        }

        public boolean hasCustomTestActions() {
            return !customTestTasks.isEmpty();
        }

        @Override
        protected JMenu createMenu() {
            JMenu menu = new JMenu(NbStrings.getCustomTestsAction());

            for (CustomTestAction action: customTestTasks) {
                menu.add(action);
            }

            return menu;
        }
    }
}
