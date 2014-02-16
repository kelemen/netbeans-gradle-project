package org.netbeans.gradle.project.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.others.GradleTestSession;
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
        GenericProjectProperties properties = javaExt.getCurrentModel().getMainModule().getProperties();
        String name = properties.getProjectName();
        return name.isEmpty() ? properties.getProjectDir().getName() : name;
    }

    private File tryGetReportDirectory() {
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

    private void displayTestSuite(final File reportFile, SAXParser parser, final GradleTestSession session) throws Exception {
        parser.reset();

        TestXmlContentHandler testXmlContentHandler = new TestXmlContentHandler(session, reportFile);
        parser.parse(reportFile, testXmlContentHandler);

        if (testXmlContentHandler.startedSuite) {
            session.endSuite(testXmlContentHandler.suiteTime);
        }
    }

    public void displayReport() {
        File[] reportFiles = getTestReportFiles();
        if (reportFiles.length == 0) {
            return;
        }

        GradleTestSession session = new GradleTestSession();
        session.newSession(getProjectName(), project);

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            parser = parserFactory.newSAXParser();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Unexpected parser configuration error.", ex);
            return;
        } catch (SAXException ex) {
            LOGGER.log(Level.WARNING, "Unexpected SAXException.", ex);
            return;
        }

        for (File reportFile: reportFiles) {
            try {
                displayTestSuite(reportFile, parser, session);
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, "Error while parsing " + reportFile, ex);
            }
        }

        session.endSession();
    }

    private class TestXmlContentHandler extends DefaultHandler {
        private final GradleTestSession session;
        private final File reportFile;

        private int level;
        private boolean startedSuite;
        private long suiteTime;
        private boolean error;
        private GradleTestSession.Testcase testcase;
        private StringBuilder failureContent;

        public TestXmlContentHandler(GradleTestSession session, File reportFile) {
            this.session = session;
            this.reportFile = reportFile;

            this.level = 0;
            this.startedSuite = false;
            this.suiteTime = 0;
            this.error = false;
            this.testcase = null;
            this.failureContent = null;
        }

        private void startSuite(Attributes attributes) {
            String name = attributes.getValue("", "name");
            suiteTime = tryReadTimeMillis(attributes.getValue("", "time"), 0);

            session.newSuite(name != null ? name : reportFile.getName());
            startedSuite = true;
        }

        private GradleTestSession.Testcase tryGetTestCase(Attributes attributes) {
            String name = attributes.getValue("", "name");
            if (name == null) {
                return null;
            }

            GradleTestSession.Testcase result = session.newTestcase(name);

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
                testcase.setStatus(GradleTestSession.Status.PASSED);

            }
            else if ("ignored-testcase".equals(qName)) {
                testcase = tryGetTestCase(attributes);
                testcase.setStatus(GradleTestSession.Status.IGNORED);
            }
        }

        private void tryUpdateTestCase(String uri, String localName, String qName, Attributes attributes) {
            if (testcase != null) {
                if ("failure".equals(qName)) {
                    error = false;
                    testcase.setStatus(GradleTestSession.Status.FAILED);
                }
                else if ("error".equals(qName)) {
                    error = true;
                    testcase.setStatus(GradleTestSession.Status.ERROR);
                }
                else {
                    LOGGER.log(Level.WARNING, "Unexpected element in testcase: {0}", qName);
                    error = true;
                    testcase.setStatus(GradleTestSession.Status.ERROR);
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
                            testcase.setTrouble(error, extractStackTrace(failureContent.toString()));
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
