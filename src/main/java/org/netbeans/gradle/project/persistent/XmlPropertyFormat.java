package org.netbeans.gradle.project.persistent;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.others.ChangeLFPlugin;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.netbeans.gradle.project.properties.AuxConfig;
import org.netbeans.gradle.project.properties.AuxConfigSource;
import org.netbeans.gradle.project.properties.ConstPropertySource;
import org.netbeans.gradle.project.properties.DefaultPropertySources;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.ProjectPlatformSource;
import org.netbeans.gradle.project.properties.PropertiesSnapshot;
import org.netbeans.gradle.project.properties.PropertySource;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class XmlPropertyFormat {
    private static final Logger LOGGER = Logger.getLogger(XmlPropertyFormat.class.getName());
    private static final String XML_ENCODING = "UTF-8";

    private static final String ROOT_NODE = "gradle-project-properties";
    private static final String SOURCE_ENCODING_NODE = "source-encoding";
    private static final String PLATFORM_NAME_NODE = "target-platform-name";
    private static final String PLATFORM_NODE = "target-platform";
    private static final String SOURCE_LEVEL_NODE = "source-level";
    private static final String COMMON_TASKS_NODE = "common-tasks";
    private static final String SCRIPT_PLATFORM_NODE = "script-platform";
    private static final String GENERIC_PLATFORM_NAME_NODE = "spec-name";
    private static final String GENERIC_PLATFORM_VERSION_NODE = "spec-version";
    private static final String GRADLE_HOME_NODE = "gradle-home";
    private static final String BUILT_IN_TASKS_NODE = "built-in-tasks";
    private static final String TASK_DISPLAY_NAME_NODE = "display-name";
    private static final String TASK_NON_BLOCKING_NODE = "non-blocking";
    private static final String TASK_NODE = "task";
    private static final String TASK_NAME_LIST_NODE = "task-names";
    private static final String TASK_NAME_NODE = "name";
    private static final String TASK_MUST_EXIST_ATTR = "must-exist";
    private static final String TASK_ARGS_NODE = "task-args";
    private static final String TASK_JVM_ARGS_NODE = "task-jvm-args";
    private static final String ARG_NODE = "arg";
    private static final String AUXILIARY_NODE = "auxiliary";
    private static final String LICENSE_HEADER_NODE = "license-header";
    private static final String LICENSE_NAME_NODE = "name";
    private static final String LICENSE_FILE_NODE = "template";
    private static final String LICENSE_PROPERTY_NODE = "property";
    private static final String LICENSE_PROPERTY_NAME_ATTR = "name";

    private static final String VALUE_YES = "yes";
    private static final String VALUE_NO = "no";

    private static final String DEFAULT_SPECIFICATION_NAME = "j2se";

    private static final String SAVE_FILE_NAME_SEPARATOR = "/";

    private static Element addChild(Node parent, String tagName) {
        Element element = parent.getOwnerDocument().createElement(tagName);
        parent.appendChild(element);
        return element;
    }

    private static Element addSimpleChild(Node parent, String tagName, String value) {
        Element element = addChild(parent, tagName);
        element.setTextContent(value);
        return element;
    }

    private static void writeBytesToFile(File outputFile, byte[] content) throws IOException {
        OutputStream output = null;
        try {
            output = new FileOutputStream(outputFile);
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static void saveDocument(NbGradleProject project, File propertyfile, Document document) throws TransformerException, IOException {
        File dir = propertyfile.getParentFile();
        if (dir != null) {
            if (!dir.mkdirs()) {
                LOGGER.log(Level.WARNING, "Cannot create directory: {0}", dir);
            }
        }

        String lineSeparator = ChangeLFPlugin.getPreferredLineSeparator(project);
        if (lineSeparator == null) {
            Result result = new StreamResult(propertyfile);
            saveDocument(result, document);
        }
        else {
            ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
            Result result = new StreamResult(output);
            saveDocument(result, document);

            String fileOutput = output.toString(XML_ENCODING);
            BufferedReader configContent = new BufferedReader(new StringReader(output.toString(XML_ENCODING)), 2048);

            StringBuilder newFileStrContent = new StringBuilder(fileOutput.length());
            for (String line = configContent.readLine(); line != null; line = configContent.readLine()) {
                newFileStrContent.append(line);
                newFileStrContent.append(lineSeparator);
            }

            writeBytesToFile(propertyfile, newFileStrContent.toString().getBytes(XML_ENCODING));
        }
    }

    private static void saveDocument(Result result, Document document) throws TransformerException, IOException {
        Source source = new DOMSource(document);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, XML_ENCODING);
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(source, result);
    }

    private static void addSingleTask(Node node, PredefinedTask task) {
        Element taskNode = addChild(node, TASK_NODE);

        addSimpleChild(taskNode, TASK_DISPLAY_NAME_NODE, task.getDisplayName());
        addSimpleChild(taskNode, TASK_NON_BLOCKING_NODE, task.isNonBlocking() ? VALUE_YES : VALUE_NO);

        Element nameListNode = addChild(taskNode, TASK_NAME_LIST_NODE);
        for (PredefinedTask.Name name: task.getTaskNames()) {
            Element nameNode = addSimpleChild(nameListNode, TASK_NAME_NODE, name.getName());
            nameNode.setAttribute(TASK_MUST_EXIST_ATTR, name.isMustExist() ? VALUE_YES : VALUE_NO);
        }

        Element argListNode = addChild(taskNode, TASK_ARGS_NODE);
        for (String arg: task.getArguments()) {
            addSimpleChild(argListNode, ARG_NODE, arg);
        }

        Element jvmArgListNode = addChild(taskNode, TASK_JVM_ARGS_NODE);
        for (String arg: task.getJvmArguments()) {
            addSimpleChild(jvmArgListNode, ARG_NODE, arg);
        }
    }

    private static void addCommonTasks(Node node, List<PredefinedTask> tasks) {
        Element commonTasksNode = addChild(node, COMMON_TASKS_NODE);

        for (PredefinedTask task: tasks) {
            addSingleTask(commonTasksNode, task);
        }
    }

    private static void addBuiltInTasks(Node node, PropertiesSnapshot snapshot) {
        Set<String> knownBuiltInCommands = snapshot.getKnownBuiltInCommands();
        List<PredefinedTask> tasks = new ArrayList<PredefinedTask>(knownBuiltInCommands.size());
        for (String command: knownBuiltInCommands) {
            PropertySource<PredefinedTask> taskProperty = snapshot.tryGetBuiltInTask(command);
            if (taskProperty != null && !taskProperty.isDefault()) {
                PredefinedTask task = taskProperty.getValue();
                if (task != null) {
                    tasks.add(task);
                }
            }
        }

        if (!tasks.isEmpty()) {
            // Sort them, so that they are saved in a deterministic order.
            Collections.sort(tasks, new Comparator<PredefinedTask>() {
                @Override
                public int compare(PredefinedTask o1, PredefinedTask o2) {
                    String displayName1 = o1.getDisplayName();
                    String displayName2 = o2.getDisplayName();
                    return displayName1.compareTo(displayName2);
                }
            });

            Element commonTasksNode = addChild(node, BUILT_IN_TASKS_NODE);
            for (PredefinedTask task: tasks) {
                addSingleTask(commonTasksNode, task);
            }
        }
    }

    private static void addGenericPlatform(Node node, String nodeName, JavaPlatform platform) {
        Element platformNode = addChild(node, nodeName);

        addSimpleChild(platformNode, GENERIC_PLATFORM_NAME_NODE, platform.getSpecification().getName());
        addSimpleChild(platformNode, GENERIC_PLATFORM_VERSION_NODE, platform.getSpecification().getVersion().toString());
    }

    private static List<AuxConfigSource> sortDomProperties(Collection<AuxConfigSource> properties) {
        List<AuxConfigSource> currentConfigs = new ArrayList<AuxConfigSource>(properties);

        // Return them sorted, so that they are saved in a deterministic order.
        Collections.sort(currentConfigs, new Comparator<AuxConfigSource>() {
            @Override
            public int compare(AuxConfigSource o1, AuxConfigSource o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        return currentConfigs;
    }

    private static void addAuxiliaryConfig(Node node, PropertiesSnapshot snapshot) {
        List<AuxConfigSource> configs = sortDomProperties(snapshot.getAuxProperties());
        if (configs.isEmpty()) {
            return;
        }

        List<Element> auxElements = new ArrayList<Element>(configs.size());
        for (AuxConfigSource config: configs) {
            Element value = config.getSource().getValue();
            if (value != null) {
                auxElements.add(value);
            }
        }

        if (auxElements.isEmpty()) {
            return;
        }

        Document doc = node.getOwnerDocument();
        Element auxiliaryNode = addChild(node, AUXILIARY_NODE);

        for (Element auxElement: auxElements) {
            auxiliaryNode.appendChild(doc.importNode(auxElement, true));
        }
    }

    private static void addFileNode(Node root, String nodeName, File file) {
        String filePathStr = file.getPath().replace(File.separator, SAVE_FILE_NAME_SEPARATOR);
        addSimpleChild(root, nodeName, filePathStr);
    }

    private static void addLicenseHeader(Node root, String nodeName, LicenseHeaderInfo licenseHeader) {
        Element licenseNode = addChild(root, nodeName);
        addSimpleChild(licenseNode, LICENSE_NAME_NODE, licenseHeader.getLicenseName());

        File templateFile = licenseHeader.getLicenseTemplateFile();
        if (templateFile != null) {
            addFileNode(licenseNode, LICENSE_FILE_NODE, templateFile);
        }

        // We sort them only to save them in a deterministic order, so the
        // property file only changes if the properties really change.
        TreeMap<String, String> sortedProperties = new TreeMap<String, String>(licenseHeader.getProperties());
        for (Map.Entry<String, String> property: sortedProperties.entrySet()) {
            Element propertyNode = addSimpleChild(licenseNode, LICENSE_PROPERTY_NODE, property.getValue());
            propertyNode.setAttribute(LICENSE_PROPERTY_NAME_ATTR, property.getKey());
        }
    }

    public static void saveToXml(NbGradleProject project, File propertyfile, PropertiesSnapshot snapshot) {
        if (propertyfile == null) throw new NullPointerException("propertyfile");
        if (snapshot == null) throw new NullPointerException("snapshot");

        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create XML builder.", ex);
            return;
        }

        Document document = builder.newDocument();
        Element root = document.createElement(ROOT_NODE);
        document.appendChild(root);

        Comment comment = document.createComment(
                "DO NOT EDIT THIS FILE!"
                + " - Used by the Gradle plugin of NetBeans.");
        root.appendChild(comment);

        if (!snapshot.getSourceEncoding().isDefault()) {
            String sourceEncoding = snapshot.getSourceEncoding().getValue().name();
            addSimpleChild(root, SOURCE_ENCODING_NODE, sourceEncoding);
        }

        if (!snapshot.getPlatform().isDefault()) {
            ProjectPlatform platform = snapshot.getPlatform().getValue();
            addSimpleChild(root, PLATFORM_NAME_NODE, platform.getName());
            addSimpleChild(root, PLATFORM_NODE, platform.getVersion());
        }

        if (!snapshot.getSourceLevel().isDefault()) {
            String sourceLevel = snapshot.getSourceLevel().getValue();
            addSimpleChild(root, SOURCE_LEVEL_NODE, sourceLevel);
        }

        if (!snapshot.getCommonTasks().isDefault()) {
            List<PredefinedTask> commonTasks = snapshot.getCommonTasks().getValue();
            if (!commonTasks.isEmpty()) {
                addCommonTasks(root, commonTasks);
            }
        }

        if (!snapshot.getScriptPlatform().isDefault()) {
            JavaPlatform scriptPlatform = snapshot.getScriptPlatform().getValue();
            addGenericPlatform(root, SCRIPT_PLATFORM_NODE, scriptPlatform);
        }

        if (!snapshot.getGradleHome().isDefault()) {
            String gradleHome = AbstractProjectProperties.gradleLocationToString(snapshot.getGradleHome().getValue());
            addSimpleChild(root, GRADLE_HOME_NODE, gradleHome);
        }

        if (!snapshot.getLicenseHeader().isDefault()) {
            LicenseHeaderInfo licenseHeader = snapshot.getLicenseHeader().getValue();
            if (licenseHeader != null) {
                addLicenseHeader(root, LICENSE_HEADER_NODE, licenseHeader);
            }
        }

        addBuiltInTasks(root, snapshot);
        addAuxiliaryConfig(root, snapshot);

        try {
            saveDocument(project, propertyfile, document);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to save the properties.", ex);
        } catch (TransformerException ex) {
            LOGGER.log(Level.INFO, "Failed to save the properties.", ex);
        }
    }

    private static Element getFirstChildByTagName(Element element, String tagName) {
        NodeList children = element.getElementsByTagName(tagName);
        return children.getLength() > 0 ? (Element)children.item(0) : null;
    }

    private static String tryGetValueOfNode(Element node, String tagName) {
        Node child = getFirstChildByTagName(node, tagName);
        String textContent = child != null ? child.getTextContent() : null;
        return textContent != null ? textContent.trim() : null;
    }


    private static Charset parseCharset(String name) {
        try {
            return Charset.forName(name);
        } catch (IllegalCharsetNameException ex) {
            LOGGER.log(Level.INFO, "The name of the character set is invalid: " + name, ex);
        } catch (UnsupportedCharsetException ex) {
            LOGGER.log(Level.INFO, "The character set is not supported: " + name, ex);
        }

        return null;
    }

    private static Iterable<Element> getChildElements(Element element, String tagName) {
        return new NodeListAsElementIterable(element.getElementsByTagName(tagName));
    }

    private static PredefinedTask readTask(Element root) {
        Element displayNameNode = getFirstChildByTagName(root, TASK_DISPLAY_NAME_NODE);
        String displayName = null;
        if (displayNameNode != null) {
            displayName = displayNameNode.getTextContent();
        }
        displayName = displayName != null ? displayName.trim() : "?";

        Element nonBlockingNode = getFirstChildByTagName(root, TASK_NON_BLOCKING_NODE);
        boolean nonBlocking = false;
        if (nonBlockingNode != null) {
            String nonBlockingStr = nonBlockingNode.getTextContent();
            if (nonBlockingStr != null) {
                nonBlocking = VALUE_YES.equalsIgnoreCase(nonBlockingStr.trim());
            }
        }

        List<PredefinedTask.Name> names = new LinkedList<PredefinedTask.Name>();
        Element nameListNode = getFirstChildByTagName(root, TASK_NAME_LIST_NODE);
        if (nameListNode != null) {
            for (Element nameNode: getChildElements(nameListNode, TASK_NAME_NODE)) {
                String name = nameNode.getTextContent();
                name = name != null ? name.trim() : "";

                if (!name.isEmpty()) {
                    boolean mustExist = VALUE_YES.equalsIgnoreCase(nameNode.getAttribute(TASK_MUST_EXIST_ATTR));
                    names.add(new PredefinedTask.Name(name, mustExist));
                }
            }
        }

        List<String> args = new LinkedList<String>();
        Element argsNode = getFirstChildByTagName(root, TASK_ARGS_NODE);
        if (argsNode != null) {
            for (Element argNode: getChildElements(argsNode, ARG_NODE)) {
                String arg = argNode.getTextContent();
                arg = arg != null ? arg.trim() : "";
                if (!arg.isEmpty()) {
                    args.add(arg);
                }
            }
        }

        List<String> jvmArgs = new LinkedList<String>();
        Element jvmArgsNode = getFirstChildByTagName(root, TASK_JVM_ARGS_NODE);
        if (jvmArgsNode != null) {
            for (Element jvmArgNode: getChildElements(jvmArgsNode, ARG_NODE)) {
                String arg = jvmArgNode.getTextContent();
                arg = arg != null ? arg.trim() : "";
                if (!arg.isEmpty()) {
                    jvmArgs.add(arg);
                }
            }
        }

        return new PredefinedTask(displayName, names, args, jvmArgs, nonBlocking);
    }

    private static List<PredefinedTask> readTasks(String rootName, Element root) {
        Element commonTasksNode = getFirstChildByTagName(root, rootName);
        if (commonTasksNode == null) {
            return Collections.emptyList();
        }

        List<PredefinedTask> result = new LinkedList<PredefinedTask>();
        for (Element taskNode: getChildElements(commonTasksNode, TASK_NODE)) {
            result.add(readTask(taskNode));
        }
        return result;
    }

    private static List<PredefinedTask> readCommonTasks(Element root) {
        return readTasks(COMMON_TASKS_NODE, root);
    }

    private static <ValueType> PropertySource<ValueType> asConst(ValueType value, boolean defaultValue) {
        return new ConstPropertySource<ValueType>(value, defaultValue);
    }

    private static List<PredefinedTask> readBuiltInTasks(Element root) {
        return readTasks(BUILT_IN_TASKS_NODE, root);
    }

    private static PropertySource<JavaPlatform> readPlatform(Element root, String nodeName) {
        Element platformNode = getFirstChildByTagName(root, nodeName);
        if (platformNode == null) {
            return null;
        }

        String platformName = tryGetValueOfNode(platformNode, GENERIC_PLATFORM_NAME_NODE);
        if (platformName == null) {
            platformName = DEFAULT_SPECIFICATION_NAME;
        }

        String versionStr = tryGetValueOfNode(platformNode, GENERIC_PLATFORM_VERSION_NODE);
        if (versionStr != null) {
            return DefaultPropertySources.findPlatformSource(platformName, versionStr, false);
        }
        return null;
    }

    private static File tryReadFilePath(Element root, String nodeName) {
        String strPath = tryGetValueOfNode(root, nodeName);
        if (strPath == null) {
            return null;
        }

        return new File(strPath.trim().replace(SAVE_FILE_NAME_SEPARATOR, File.separator));
    }

    private static PropertySource<LicenseHeaderInfo> readLicenseHeader(Element root, String nodeName) {
        Element licenseNode = getFirstChildByTagName(root, nodeName);
        if (licenseNode == null) {
            return null;
        }

        Element nameNode = getFirstChildByTagName(licenseNode, LICENSE_NAME_NODE);
        if (nameNode == null) {
            return null;
        }

        String name = nameNode.getTextContent();
        if (name == null) {
            return null;
        }

        File licenseTemplate = tryReadFilePath(licenseNode, LICENSE_FILE_NODE);

        Map<String, String> properties = new TreeMap<String, String>();
        NodeList childNodes = licenseNode.getChildNodes();
        int childCount = childNodes.getLength();
        for (int i = 0; i < childCount; i++) {
            Node property = childNodes.item(i);
            Element propertyElement = property instanceof Element
                    ? (Element)property
                    : null;

            if (propertyElement != null && LICENSE_PROPERTY_NODE.equals(propertyElement.getNodeName())) {
                String propertyName = propertyElement.getAttribute(LICENSE_PROPERTY_NAME_ATTR);
                String properyValue = property.getTextContent();

                if (propertyName != null && properyValue != null) {
                    properties.put(propertyName.trim(), properyValue.trim());
                }
            }
        }

        return asConst(new LicenseHeaderInfo(name.trim(), properties, licenseTemplate), false);
    }

    private static Collection<AuxConfig> readAuxiliaryConfigs(Element root) {
        List<AuxConfig> result = new LinkedList<AuxConfig>();

        Element auxNode = getFirstChildByTagName(root, AUXILIARY_NODE);
        if (auxNode == null) {
            return result;
        }

        NodeList childNodes = auxNode.getChildNodes();
        int childCount = childNodes.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                String elementName = child.getNodeName();
                String namespace = child.getNamespaceURI();
                result.add(new AuxConfig(elementName, namespace, (Element)child));
            }
        }
        return result;
    }

    public static PropertiesSnapshot readFromXml(File propertiesFile) {
        PropertiesSnapshot.Builder result = new PropertiesSnapshot.Builder();

        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create XML builder.", ex);
            return result.create();
        }

        Document document;
        try {
            if (!propertiesFile.exists()) {
                return result.create();
            }

            document = builder.parse(propertiesFile);
        } catch (SAXException ex) {
            LOGGER.log(Level.INFO, "Failed to parse the property file.", ex);
            return result.create();
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to read the property file.", ex);
            return result.create();
        }

        Element root = document.getDocumentElement();

        String sourceLevel = tryGetValueOfNode(root, SOURCE_LEVEL_NODE);
        if (sourceLevel != null) {
            result.setSourceLevel(asConst(sourceLevel, false));
        }

        String sourceEncodingStr = tryGetValueOfNode(root, SOURCE_ENCODING_NODE);
        Charset sourceEncoding = sourceEncodingStr != null
                ? parseCharset(sourceEncodingStr)
                : null;
        if (sourceEncoding != null) {
            result.setSourceEncoding(asConst(sourceEncoding, false));
        }

        String platformName = tryGetValueOfNode(root, PLATFORM_NAME_NODE);
        if (platformName == null) {
            platformName = DEFAULT_SPECIFICATION_NAME;
        }

        String platformVersion = tryGetValueOfNode(root, PLATFORM_NODE);
        if (platformVersion != null) {
            result.setPlatform(new ProjectPlatformSource(platformName, platformVersion, false));
        }

        String gradleHomeStr = tryGetValueOfNode(root, GRADLE_HOME_NODE);
        if (gradleHomeStr != null) {
            GradleLocation gradleHome = AbstractProjectProperties.getGradleLocationFromString(gradleHomeStr);
            result.setGradleHome(asConst(gradleHome, false));
        }

        PropertySource<JavaPlatform> scriptPlatform = readPlatform(root, SCRIPT_PLATFORM_NODE);
        if (scriptPlatform != null) {
            result.setScriptPlatform(scriptPlatform);
        }

        PropertySource<LicenseHeaderInfo> licenseHeader = readLicenseHeader(root, LICENSE_HEADER_NODE);
        if (licenseHeader != null) {
            result.setLicenseHeader(licenseHeader);
        }

        List<PredefinedTask> commonTasks = Collections.unmodifiableList(readCommonTasks(root));
        result.setCommonTasks(asConst(commonTasks, commonTasks.isEmpty()));

        for (PredefinedTask builtInTask: readBuiltInTasks(root)) {
            String command = builtInTask.getDisplayName();
            result.setBuiltInTask(command, asConst(builtInTask, false));
        }

        for (AuxConfig auxConfig: readAuxiliaryConfigs(root)) {
            result.addAuxConfig(auxConfig, false);
        }

        return result.create();
    }

    private static class NodeListAsElementIterable implements Iterable<Element> {
        private final NodeListAsIterable wrapped;

        public NodeListAsElementIterable(NodeList nodeList) {
            this.wrapped = new NodeListAsIterable(nodeList);
        }

        @Override
        public Iterator<Element> iterator() {
            final Iterator<Node> nodeItr = wrapped.iterator();

            return new Iterator<Element>() {
                @Override
                public boolean hasNext() {
                    return nodeItr.hasNext();
                }

                @Override
                @SuppressWarnings("unchecked")
                public Element next() {
                    return (Element)nodeItr.next();
                }

                @Override
                public void remove() {
                    nodeItr.remove();
                }
            };
        }
    }

    private static class NodeListAsIterable implements Iterable<Node> {
        private final NodeList nodeList;

        public NodeListAsIterable(NodeList nodeList) {
            if (nodeList == null) throw new NullPointerException("nodeList");
            this.nodeList = nodeList;
        }

        @Override
        public Iterator<Node> iterator() {
            final int numberOfNodes = nodeList.getLength();
            return new Iterator<Node>() {
                private int currentIndex = 0;

                @Override
                public boolean hasNext() {
                    return currentIndex < numberOfNodes;
                }

                @Override
                public Node next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    Node result = nodeList.item(currentIndex);
                    currentIndex++;
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("This Iterable is read-only.");
                }
            };
        }
    }

    private XmlPropertyFormat() {
        throw new AssertionError();
    }
}
