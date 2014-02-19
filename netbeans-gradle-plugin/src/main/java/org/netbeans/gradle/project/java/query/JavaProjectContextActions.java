package org.netbeans.gradle.project.java.query;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.StringUtils;
import org.netbeans.gradle.project.api.nodes.GradleActionType;
import org.netbeans.gradle.project.api.nodes.GradleProjectAction;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.tasks.TestTaskName;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;

public final class JavaProjectContextActions implements GradleProjectContextActions {
    private static final Logger LOGGER = Logger.getLogger(JavaProjectContextActions.class.getName());

    private final JavaExtension javaExt;

    public JavaProjectContextActions(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    private void addCustomTestTasks(List<Action> result) {
        JavaTestModel testTasks = javaExt.getCurrentModel().getMainModule().getTestTasks();
        for (JavaTestTask testTask: testTasks.getTestTasks()) {
            if (!TestTaskName.DEFAULT_TEST_TASK_NAME.equals(testTask.getName())) {
                result.add(new CustomTestAction(testTask));
            }
        }
    }

    @Override
    public List<Action> getContextActions() {
        List<Action> result = new LinkedList<Action>();

        addCustomTestTasks(result);
        result.add(createJavaDocAction());
        result.add(sourcesDirsAction());

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
        String capitalizedName = StringUtils.capitalizeFirstCharacter(testTask.getName());
        return NbStrings.getCustomTestCommandCaption(capitalizedName);
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

    private static String[] safeGetSupportedActions(ActionProvider provider) {
        String[] result = provider.getSupportedActions();
        return result != null ? result : new String[0];
    }

    private static boolean supportsAction(ActionProvider provider, String command) {
        for (String action: safeGetSupportedActions(provider)) {
            if (command.equals(action)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryInvokeAction(Project project, String command, Lookup context) {
        Lookup projectLookup = project.getLookup();
        for (ActionProvider actionProvider: projectLookup.lookupAll(ActionProvider.class)) {
            if (supportsAction(actionProvider, command)) {
                actionProvider.invokeAction(command, context);
                return true;
            }
        }
        return false;
    }

    private static void invokeAction(Project project, String command, Lookup context) {
        if (!tryInvokeAction(project, command, context)) {
            LOGGER.log(Level.WARNING,
                    "Could not invoke command {0} for project {1}",
                    new Object[]{command, project.getProjectDirectory()});
        }
    }

    private final class CustomTestAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private final Lookup context;

        public CustomTestAction(JavaTestTask testTask) {
            super(getCustomTestTaskName(testTask));

            this.context = Lookups.singleton(new TestTaskName(testTask.getName()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            invokeAction(javaExt.getProject(), ActionProvider.COMMAND_TEST, context);
        }
    }

    @SuppressWarnings("serial") // don't care about serialization
    @GradleProjectAction(GradleActionType.PROJECT_MANAGEMENT_ACTION)
    private class SourceDirsAction extends AbstractAction implements Presenter.Popup {
        private JMenu cachedMenu;

        public SourceDirsAction() {
            this.cachedMenu = null;
        }

        @Override
        public JMenuItem getPopupPresenter() {
            if (cachedMenu == null) {
                cachedMenu = createMenu();
            }
            return cachedMenu;
        }

        private JMenu createMenu() {
            JMenu menu = new JMenu(NbStrings.getSourceDirsActionGroup());
            menu.add(backgroundTaskMenuItem(NbStrings.getCreateSourceDirsAction(), new Runnable() {
                @Override
                public void run() {
                    javaExt.getSourceDirsHandler().createDirectories();
                }
            }));
            menu.add(backgroundTaskMenuItem(NbStrings.getDeleteEmptySourceDirsAction(), new Runnable() {
                @Override
                public void run() {
                    javaExt.getSourceDirsHandler().deleteEmptyDirectories();
                }
            }));
            return menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }
}
