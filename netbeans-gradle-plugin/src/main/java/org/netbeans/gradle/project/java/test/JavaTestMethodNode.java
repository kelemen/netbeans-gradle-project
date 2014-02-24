package org.netbeans.gradle.project.java.test;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.modules.gsf.testrunner.api.TestMethodNode;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.spi.project.SingleMethod;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class JavaTestMethodNode extends TestMethodNode {
    private final JavaExtension javaExt;
    private final TestTaskName testTaskName;
    private final SpecificTestcase specificTestcase;

    public JavaTestMethodNode(
            Testcase testcase,
            JavaExtension javaExt,
            TestTaskName testTaskName) {
        super(testcase, javaExt.getProject());

        if (javaExt == null) throw new NullPointerException("javaExt");
        if (testTaskName == null) throw new NullPointerException("testTaskName");

        this.javaExt = javaExt;
        this.testTaskName = testTaskName;
        this.specificTestcase = toSpecificTestcase(testcase);
    }

    private static SpecificTestcase toSpecificTestcase(Testcase testcase) {
        String className = testcase.getClassName();
        String name = testcase.getName();

        if (className != null && name != null) {
            return new SpecificTestcase(className, name);
        }
        else {
            return null;
        }
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            runMethodAction(),
            debugMethodAction(),
        };
    }

    private Action runMethodAction() {
        return new RerunTestMethodAction(
                NbStrings.getTestSingleMethodAgain(),
                SingleMethod.COMMAND_RUN_SINGLE_METHOD);
    }

    private Action debugMethodAction() {
        return new RerunTestMethodAction(
                NbStrings.getDebugTestSingleMethodAgain(),
                SingleMethod.COMMAND_DEBUG_SINGLE_METHOD);
    }

    @SuppressWarnings("serial")
    private class RerunTestMethodAction extends AbstractAction {
        private final String command;

        public RerunTestMethodAction(String name, String command) {
            super(name);

            this.command = command;
            if (specificTestcase == null) {
                setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (specificTestcase == null) {
                return;
            }

            Project project = javaExt.getProject();
            Lookup context = Lookups.fixed(testTaskName, specificTestcase);

            GradleActionProvider.invokeAction(project, command, context);
        }
    }
}
