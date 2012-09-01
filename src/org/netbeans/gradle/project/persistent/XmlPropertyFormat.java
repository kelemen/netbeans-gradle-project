package org.netbeans.gradle.project.persistent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
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
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.properties.MemProjectProperties;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.PropertiesSnapshot;
import org.openide.modules.SpecificationVersion;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class XmlPropertyFormat {
    private static final Logger LOGGER = Logger.getLogger(XmlPropertyFormat.class.getName());

    private static final String ROOT_NODE = "gradle-project-properties";
    private static final String SOURCE_ENCODING_NODE = "source-encoding";
    private static final String PLATFORM_NODE = "target-platform";
    private static final String SOURCE_LEVEL_NODE = "source-level";
    private static final String COMMON_TASKS_NODE = "common-tasks";
    private static final String TASK_DISPLAY_NAME_NODE = "display-name";
    private static final String TASK_NODE = "task";
    private static final String TASK_NAME_LIST_NODE = "task-names";
    private static final String TASK_NAME_NODE = "name";
    private static final String TASK_MUST_EXIST_ATTR = "must-exist";
    private static final String TASK_ARGS_NODE = "task-args";
    private static final String TASK_JVM_ARGS_NODE = "task-jvm-args";
    private static final String ARG_NODE = "arg";

    private static final String VALUE_YES = "yes";
    private static final String VALUE_NO = "no";

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

    private static void saveDocument(File propertyfile, Document document) throws TransformerException, IOException {
        Source source = new DOMSource(document);
        Result result = new StreamResult(propertyfile);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(source, result);
    }

    private static void addSingleTask(Node node, PredefinedTask task) {
        Element taskNode = addChild(node, TASK_NODE);

        addSimpleChild(taskNode, TASK_DISPLAY_NAME_NODE, task.getDisplayName());

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

    public static void saveToXml(File propertyfile, PropertiesSnapshot snapshot) {
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

        JavaPlatform defaultPlatform = JavaPlatform.getDefault();

        String sourceEncoding = snapshot.getSourceEncoding().name();
        if (!sourceEncoding.equals(MemProjectProperties.DEFAULT_SOURCE_ENCODING.name())) {
            addSimpleChild(root, SOURCE_ENCODING_NODE, sourceEncoding);
        }

        JavaPlatform platform = snapshot.getPlatform();
        if (!platform.equals(defaultPlatform)) {
            addSimpleChild(root, PLATFORM_NODE, platform.getSpecification().getVersion().toString());
        }

        String sourceLevel = snapshot.getSourceLevel();
        if (!sourceLevel.equals(MemProjectProperties.getSourceLevelFromPlatform(defaultPlatform))) {
            addSimpleChild(root, SOURCE_LEVEL_NODE, sourceLevel);
        }

        List<PredefinedTask> commonTasks = snapshot.getCommonTasks();
        if (!commonTasks.isEmpty()) {
            addCommonTasks(root, commonTasks);
        }

        try {
            saveDocument(propertyfile, document);
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

    private static JavaPlatform parsePlatform(String versionStr) {
        SpecificationVersion version;
        try {
            version = new SpecificationVersion(versionStr);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.INFO, "Invalid platform version: " + versionStr, ex);
            return null;
        }

        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        for (JavaPlatform platform: platforms) {
            if (version.equals(platform.getSpecification().getVersion())) {
                return platform;
            }
        }

        // We could not find an exact match, so try to find the best match:
        //
        // 1. If there is at least one platform with a version higher than
        //    requested, choose the one with the lowest version which is still
        //    higher than the requested (the closest version to the requested
        //    which is above the requested version).
        //
        // 2. In case every platform is below the requested, choose the one
        //    with the highest version number.

        JavaPlatform bestMatch = null;
        for (JavaPlatform platform: platforms) {
            if (bestMatch == null) {
                bestMatch = platform;
            }
            else {
                SpecificationVersion bestVersion = bestMatch.getSpecification().getVersion();
                SpecificationVersion thisVersion = platform.getSpecification().getVersion();

                // required version is greater than the one we currently have
                if (version.compareTo(bestVersion) > 0) {
                    // Replace if this platform has a greater version number
                    if (bestVersion.compareTo(thisVersion) < 0) {
                        bestMatch = platform;
                    }
                }
                else {
                    // Replace if this platform is still above the requirement
                    // but is below the one we currently have.
                    if (version.compareTo(thisVersion) < 0
                            && thisVersion.compareTo(bestVersion) < 0) {
                        bestMatch = platform;
                    }
                }
            }
        }

        if (bestMatch == null) {
            LOGGER.severe("Could not find any Java platform.");
        }
        else if (version.compareTo(bestMatch.getSpecification().getVersion()) > 0) {
            LOGGER.log(Level.WARNING,
                    "The choosen platform has a higher version number than the requested one: {0}",
                    versionStr);
        }
        else {
            LOGGER.log(Level.WARNING,
                    "The choosen platform has a lower version number than the requested one: {0}",
                    versionStr);
        }
        return bestMatch;
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

        List<PredefinedTask.Name> names = new LinkedList<PredefinedTask.Name>();
        Element nameListNode = getFirstChildByTagName(root, TASK_NAME_LIST_NODE);
        if (nameListNode != null) {
            for (Element nameNode: getChildElements(nameListNode, TASK_NAME_NODE)) {
                String name = nameNode.getTextContent();
                name = name != null ? name.trim() : "";

                if (!name.isEmpty()) {
                    boolean mustExist = VALUE_YES.equals(nameNode.getAttribute(TASK_MUST_EXIST_ATTR));
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

        return new PredefinedTask(displayName, names, args, jvmArgs);
    }

    private static List<PredefinedTask> readTasks(Element root) {
        Element commonTasksNode = getFirstChildByTagName(root, COMMON_TASKS_NODE);
        if (commonTasksNode == null) {
            return Collections.emptyList();
        }

        List<PredefinedTask> result = new LinkedList<PredefinedTask>();
        for (Element taskNode: getChildElements(commonTasksNode, TASK_NODE)) {
            result.add(readTask(taskNode));
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
            result.setSourceLevel(sourceLevel);
        }

        String sourceEncodingStr = tryGetValueOfNode(root, SOURCE_ENCODING_NODE);
        Charset sourceEncoding = sourceEncodingStr != null
                ? parseCharset(sourceEncodingStr)
                : null;
        if (sourceEncoding != null) {
            result.setSourceEncoding(sourceEncoding);
        }

        String platformStr = tryGetValueOfNode(root, PLATFORM_NODE);
        JavaPlatform platform = platformStr != null
                ? parsePlatform(platformStr)
                : null;
        if (platform != null) {
            result.setPlatform(platform);
        }

        List<PredefinedTask> commonTasks = readTasks(root);
        result.setCommonTasks(commonTasks);

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
