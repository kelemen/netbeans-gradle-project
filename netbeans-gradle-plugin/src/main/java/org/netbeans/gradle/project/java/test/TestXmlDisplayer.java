package org.netbeans.gradle.project.java.test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.others.test.NbGradleTestManager;
import org.netbeans.gradle.project.others.test.NbGradleTestManagers;
import org.netbeans.gradle.project.others.test.NbGradleTestSession;
import org.netbeans.gradle.project.others.test.NbGradleTestSuite;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.RerunType;
import org.netbeans.modules.gsf.testrunner.api.Status;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.Trouble;
import org.netbeans.spi.project.ActionProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class TestXmlDisplayer {
    private static final Logger LOGGER = Logger.getLogger(TestXmlDisplayer.class.getName());
    private static final File[] NO_FILES = new File[0];
    private static final String NEW_LINE_PATTERN = Pattern.quote("\n");
    private static final String[] STACKTRACE_PREFIXES = {"at "};

    private final Project project;
    private final JavaExtension javaExt;
    private final String testName;

    public TestXmlDisplayer(Project project, String testName) {
        if (project == null) throw new NullPointerException("project");
        if (testName == null) throw new NullPointerException("testName");

        this.project = project;
        this.testName = testName;
        this.javaExt = JavaExtension.getJavaExtensionOfProject(project);
    }

    private String getProjectName() {
        ProjectInformation projectInfo = ProjectUtils.getInformation(project);
        return projectInfo.getDisplayName();
    }

    public String getTestName() {
        return testName;
    }

    public File tryGetReportDirectory() {
        JavaTestTask testTask = javaExt.getCurrentModel().getMainModule().tryGetTestModelByName(testName);
        if (testTask == null) {
            testTask = JavaTestTask.getDefaulTestModel(javaExt.getProjectDirectoryAsFile());
        }

        return testTask.getXmlOutputDir();
    }

    private File[] getTestReportFiles() {
        File reportDir = tryGetReportDirectory();
        if (reportDir == null) {
            return NO_FILES;
        }

        File[] result = reportDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String normName = name.toLowerCase(Locale.ROOT);
                return normName.startsWith("test-") && normName.endsWith(".xml");
            }
        });

        return result != null ? result : NO_FILES;
    }

    private static long tryReadTimeMillis(String timeStr, long defaultValue) {
        if (timeStr == null) {
            return defaultValue;
        }

        try {
            return Math.round(Double.parseDouble(timeStr) * 1000.0);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String[] toLines(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()
                .split(NEW_LINE_PATTERN);
    }

    private static String[] extractStackTrace(String text) {
        String[] lines = toLines(text);

        // The first line is the exception message.
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            for (String prefix: STACKTRACE_PREFIXES) {
                if (line.startsWith(prefix)) {
                    line = line.substring(prefix.length());
                    break;
                }
            }
            lines[i] = line;
        }
        return lines;
    }

    private void displayTestSuite(final File reportFile, SAXParser parser, final NbGradleTestSession testSession) throws Exception {
        parser.reset();

        TestXmlContentHandler testXmlContentHandler = new TestXmlContentHandler(testSession, reportFile);
        parser.parse(reportFile, testXmlContentHandler);

        NbGradleTestSuite testSuite = testXmlContentHandler.testSuite;
        if (testSuite != null) {
            testSuite.endSuite(testXmlContentHandler.suiteTime);
        }
    }

    private SAXParser tryGetSaxParser() {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        try {
            return parserFactory.newSAXParser();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Unexpected parser configuration error.", ex);
            return null;
        } catch (SAXException ex) {
            LOGGER.log(Level.WARNING, "Unexpected SAXException.", ex);
            return null;
        }
    }

    private boolean displayTestSession(NbGradleTestSession testSession, File[] reportFiles) {
        SAXParser parser = tryGetSaxParser();
        if (parser == null) {
            return false;
        }

        for (File reportFile: reportFiles) {
            try {
                displayTestSuite(reportFile, parser, testSession);
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, "Error while parsing " + reportFile, ex);
            }
        }

        return true;
    }

    private boolean displayReport(
            Lookup runContext,
            NbGradleTestManager testManager,
            File[] reportFiles) {

        NbGradleTestSession testSession = testManager.startSession(
                getProjectName(),
                project,
                new JavaTestRunnerNodeFactory(javaExt, new TestTaskName(testName)),
                new JavaRerunHandler(runContext));

        try {
            return displayTestSession(testSession, reportFiles);
        } finally {
            testSession.endSession();
        }
    }

    public boolean displayReport(Lookup runContext) {
        if (runContext == null) throw new NullPointerException("runContext");

        File[] reportFiles = getTestReportFiles();
        if (reportFiles.length == 0) {
            LOGGER.log(Level.WARNING,
                    "Could not find output for test task \"{0}\" in {1}",
                    new Object[]{testName, tryGetReportDirectory()});
            return false;
        }

        NbGradleTestManager testManager = NbGradleTestManagers.getTestManager();
        return displayReport(runContext, testManager, reportFiles);
    }

    public class JavaRerunHandler implements RerunHandler {
        private final Lookup rerunContext;

        private JavaRerunHandler(Lookup rerunContext) {
            this.rerunContext = rerunContext;
        }

        @Override
        public void rerun() {
            String commandStr = GradleActionProvider.getCommandStr(rerunContext, ActionProvider.COMMAND_TEST);
            GradleActionProvider.invokeAction(project, commandStr, rerunContext);
        }

        private List<SpecificTestcase> getSpecificTestcases(Set<Testcase> tests) {
            List<SpecificTestcase> result = new ArrayList<SpecificTestcase>(tests.size());
            for (Testcase test: tests) {
                String name = test.getName();
                String testClassName = test.getClassName();

                if (name != null && testClassName != null) {
                    result.add(new SpecificTestcase(testClassName, testName));
                }
            }
            return result;
        }

        @Override
        public void rerun(Set<Testcase> tests) {
            if (tests.isEmpty()) {
                LOGGER.warning("Rerun test requested with an empty test set.");
                return;
            }

            SpecificTestcases testcases = new SpecificTestcases(getSpecificTestcases(tests));
            Lookup context = Lookups.fixed(new TestTaskName(testName), testcases);
            GradleActionProvider.invokeAction(project, ActionProvider.COMMAND_TEST, context);
        }

        @Override
        public boolean enabled(RerunType type) {
            return type == RerunType.ALL;
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
        }
    }

    private class TestXmlContentHandler extends DefaultHandler {
        private final NbGradleTestSession session;
        private final File reportFile;

        private int level;
        private NbGradleTestSuite testSuite;
        private long suiteTime;
        private boolean error;
        private Testcase testcase;
        private StringBuilder failureContent;

        public TestXmlContentHandler(NbGradleTestSession session, File reportFile) {
            this.session = session;
            this.reportFile = reportFile;

            this.level = 0;
            this.testSuite = null;
            this.suiteTime = 0;
            this.error = false;
            this.testcase = null;
            this.failureContent = null;
        }

        private void startSuite(Attributes attributes) {
            String name = attributes.getValue("", "name");
            suiteTime = tryReadTimeMillis(attributes.getValue("", "time"), 0);

            String suiteName = name != null ? name : reportFile.getName();
            testSuite = session.startTestSuite(suiteName);
        }

        private Testcase tryGetTestCase(Attributes attributes) {
            if (testSuite == null) {
                LOGGER.warning("test suite has not been started but there is a test case to add.");
                return null;
            }

            String name = attributes.getValue("", "name");
            if (name == null) {
                return null;
            }

            Testcase result = testSuite.addTestcase(name);

            String className = attributes.getValue("", "classname");
            if (className != null) {
                result.setClassName(className);
            }

            long time = tryReadTimeMillis(attributes.getValue("", "time"), 0);
            result.setTimeMillis(time);

            return result;
        }

        private void tryAddTestCase(String uri, String localName, String qName, Attributes attributes) {
            if ("testcase".equals(qName)) {
                testcase = tryGetTestCase(attributes);
                testcase.setStatus(Status.PASSED);

            }
            else if ("ignored-testcase".equals(qName)) {
                testcase = tryGetTestCase(attributes);
                testcase.setStatus(Status.IGNORED);
            }
        }

        private void tryUpdateTestCase(String uri, String localName, String qName, Attributes attributes) {
            if (testcase != null) {
                if ("failure".equals(qName)) {
                    error = false;
                    testcase.setStatus(Status.FAILED);
                }
                else if ("error".equals(qName)) {
                    error = true;
                    testcase.setStatus(Status.ERROR);
                }
                else {
                    LOGGER.log(Level.WARNING, "Unexpected element in testcase: {0}", qName);
                    error = true;
                    testcase.setStatus(Status.ERROR);
                }
                failureContent = new StringBuilder(1024);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (level) {
                case 0:
                    startSuite(attributes);
                    break;
                case 1:
                    tryAddTestCase(uri, localName, qName, attributes);
                    break;
                case 2:
                    tryUpdateTestCase(uri, localName, qName, attributes);
                    break;
            }

            level++;
        }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                level--;

                switch (level) {
                    case 1:
                        testcase = null;
                        break;
                    case 2:
                        if (failureContent != null && testcase != null) {
                            Trouble trouble = new Trouble(error);
                            trouble.setStackTrace(extractStackTrace(failureContent.toString()));
                            testcase.setTrouble(trouble);
                        }
                        failureContent = null;
                        break;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (failureContent != null) {
                    failureContent.append(ch, start, length);
                }
            }
    }
}
