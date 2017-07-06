package org.netbeans.gradle.project.properties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.jtrim2.collections.CollectionsEx;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.others.ChangeLFPlugin;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class ConfigXmlUtils {
    private static final Logger LOGGER = Logger.getLogger(ConfigXmlUtils.class.getName());

    public static final String AUXILIARY_NODE_NAME = "auxiliary";

    private static final String XML_ENCODING = "UTF-8";
    private static final int FILE_BUFFER_SIZE = 8 * 1024;

    private static final char ESCAPE_START_CHAR = '_';
    private static final char ESCAPE_END_CHAR = '.';
    private static final String EMPTY_ESCAPE = ESCAPE_START_CHAR + "" + ESCAPE_END_CHAR;

    private static final String KEYWORD_PREFIX = "__";
    private static final String KEYWORD_VALUE = KEYWORD_PREFIX + "value";
    private static final String KEYWORD_HAS_VALUE = KEYWORD_PREFIX + "has-value";

    private static final String STR_NO = "no";

    private static final String ATTR_PREFIX = "#attr-";

    private static String asAttributeName(String keyName) {
        return ATTR_PREFIX + keyName;
    }

    private static boolean containsChar(String str, char ch) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                return true;
            }
        }
        return false;
    }

    private static int addEscaped(String str, int offset, StringBuilder result) {
        int length = str.length();
        if (length <= offset) {
            return 0;
        }

        if (str.charAt(offset) == ESCAPE_START_CHAR) {
            result.append(ESCAPE_START_CHAR);
            return 1;
        }

        int charValue = 0;
        int skippedCount = 0;
        boolean hasNumber = false;
        for (int i = offset; i < length; i++) {
            char ch = str.charAt(i);
            skippedCount++;

            if (ch >= '0' && ch <= '9') {
                hasNumber = true;
                charValue = 10 * charValue + (ch - '0');
            }
            else {
                break;
            }
        }

        if (hasNumber) {
            result.append((char)charValue);
        }
        return skippedCount;
    }

    private static String fromElementName(String elementName) {
        if (!containsChar(elementName, ESCAPE_START_CHAR)) {
            return elementName;
        }

        int nameLength = elementName.length();
        StringBuilder result = new StringBuilder(nameLength);

        int index = 0;
        while (index < nameLength) {
            char ch = elementName.charAt(index);
            if (ch == ESCAPE_START_CHAR) {
                index++;
                index += addEscaped(elementName, index, result);
            }
            else {
                result.append(ch);
                index++;
            }
        }

        return result.toString();
    }

    private static boolean isLowerCaseLetter(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    private static boolean isUpperCaseLetter(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    private static boolean isLetter(char ch) {
        return isLowerCaseLetter(ch) || isUpperCaseLetter(ch);
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static boolean isValidFirstElementChar(char ch) {
        return isLetter(ch);
    }

    private static boolean isValidElementChar(char ch) {
        if (isValidFirstElementChar(ch)) {
            return true;
        }
        if (isDigit(ch)) {
            return true;
        }

        return ch == '.' || ch == '-';
    }

    private static boolean startsWithXmlReserved(String name) {
        String reserved = "xml";
        if (name.length() < reserved.length()) {
            return false;
        }

        for (int i = 0; i < reserved.length(); i++) {
            char ch = Character.toLowerCase(name.charAt(i));
            if (ch != reserved.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidTagNameStart(String name) {
        if (name.isEmpty()) {
            return false;
        }

        if (startsWithXmlReserved(name)) {
            return false;
        }

        return isValidFirstElementChar(name.charAt(0));
    }


    private static String toElementName(String rawName) {
        int nameLength = rawName.length();

        if (nameLength == 0) {
            return EMPTY_ESCAPE;
        }

        StringBuilder result = new StringBuilder(nameLength);

        if (!isValidTagNameStart(rawName)) {
            result.append(EMPTY_ESCAPE);
        }

        for (int i = 0; i < nameLength; i++) {
            char ch = rawName.charAt(i);

            if (ch == ESCAPE_START_CHAR) {
                result.append(ESCAPE_START_CHAR);
                result.append(ESCAPE_START_CHAR);
            }
            else if (isValidElementChar(ch)) {
                result.append(ch);
            }
            else {
                result.append(ESCAPE_START_CHAR);
                result.append((int)ch);
                result.append(ESCAPE_END_CHAR);
            }
        }

        return result.toString();
    }

    private static boolean addAttributes(Element element, ConfigTree.Builder result) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) {
            return false;
        }

        boolean setValue = false;

        int attributeCount = attributes.getLength();
        for (int i = 0; i < attributeCount; i++) {
            Node attribute = attributes.item(i);

            String xmlAttrName = attribute.getNodeName();
            String attrValue = attribute.getNodeValue();

            if (xmlAttrName.startsWith(KEYWORD_PREFIX)) {
                switch (xmlAttrName) {
                    case KEYWORD_VALUE:
                        result.setValue(attrValue);
                        setValue = true;
                        break;
                    case KEYWORD_HAS_VALUE:
                        if (STR_NO.equals(attrValue)) {
                            result.setValue(null);
                            setValue = true;
                        }
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "Unknown keyword in properties file: {0}", xmlAttrName);
                        break;
                }
            }
            else {
                String attrName = fromElementName(xmlAttrName);
                result.addChildBuilder(asAttributeName(attrName)).setValue(attrValue);
            }
        }
        return setValue;
    }

    private static int addChildren(Element element, Set<String> excludedNames, ConfigTree.Builder result) {
        NodeList childNodes = element.getChildNodes();

        int addedChildren = 0;
        int childCount = childNodes.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                Element elementChild = (Element)child;

                String rawNodeName = elementChild.getNodeName();
                if (excludedNames.contains(rawNodeName)) {
                    continue;
                }

                String elementKey = fromElementName(elementChild.getNodeName());

                ConfigTree.Builder childBuilder = result.addChildBuilder(elementKey);
                String nodeValue = parseNode(elementChild, Collections.<String>emptySet(), childBuilder);
                if (nodeValue != null) {
                    childBuilder.setValue(nodeValue);
                }

                addedChildren++;
            }
        }

        return addedChildren;
    }

    private static String parseNode(Element root, Set<String> excludedNames, ConfigTree.Builder result) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(result, "result");

        boolean setValue = addAttributes(root, result);

        int addedChildCount = addChildren(root, excludedNames, result);
        if (!setValue && addedChildCount == 0) {
            return root.getTextContent();
        }
        else {
            return null;
        }
    }

    public static ConfigTree.Builder parseDocument(Document document, String... excludedNames) {
        ConfigTree.Builder result = new ConfigTree.Builder();

        Element root = document.getDocumentElement();
        if (root != null) {
            parseNode(root, new HashSet<>(Arrays.asList(excludedNames)), result);
        }

        return result;
    }

    private static List<KeyValuePair> tryGetAttributeList(ConfigTree tree) {
        List<KeyValuePair> attributes = null;
        for (Map.Entry<String, List<ConfigTree>> entry: tree.getChildTrees().entrySet()) {
            List<ConfigTree> childList = entry.getValue();
            if (childList.size() != 1) {
                continue;
            }

            ConfigTree child = childList.get(0);
            if (!child.getChildTrees().isEmpty()) {
                continue;
            }

            String key = entry.getKey();
            if (!key.startsWith(ATTR_PREFIX)) {
                continue;
            }

            if (attributes == null) {
                attributes = new ArrayList<>(tree.getChildTrees().size());
            }

            // A childless ConfigTree should always have a value, this is
            // something guaranteed by ConfigTree.
            String value = child.getValue("");
            attributes.add(new KeyValuePair(key, value));
        }
        return attributes;
    }

    private static Set<String> addAttributeChildrenToXml(
            Element parent,
            ConfigTree tree,
            final ConfigNodeProperty nodeSorter) {

        List<KeyValuePair> attributes = tryGetAttributeList(tree);
        if (attributes == null) {
            return Collections.emptySet();
        }

        // Despite that attributes are unordered, add them in a deterministic
        // order, so that any sensible implementation will save them in the
        // same order every time (avoiding unnecessary differences in the
        // properties file).
        Collections.sort(attributes, (o1, o2) -> nodeSorter.compare(o1.key, o2.key));

        Set<String> result = CollectionsEx.newHashSet(attributes.size());
        for (KeyValuePair keyValue: attributes) {
            String key = keyValue.key;
            result.add(key);

            assert key.startsWith(ATTR_PREFIX);
            String xmlKey = toElementName(key.substring(ATTR_PREFIX.length()));
            parent.setAttribute(xmlKey, keyValue.value);
        }
        return result;
    }

    private static void addTreeToXml(
            Document document,
            Element parent,
            ConfigTree tree,
            final ConfigNodeProperty nodeProperties) {

        Map<String, List<ConfigTree>> children = tree.getChildTrees();

        String value = tree.getValue(null);
        boolean ignoreValue = nodeProperties.ignoreValue();
        if (ignoreValue) {
            value = null;
        }

        if (value != null) {
            if (children.isEmpty()) {
                parent.setTextContent(value);
                return;
            }
        }

        Set<String> attributeKeys = addAttributeChildrenToXml(parent, tree, nodeProperties);

        List<NamedNode> childEntries = new ArrayList<>(children.size());
        for (Map.Entry<String, List<ConfigTree>> entry: children.entrySet()) {
            String key = entry.getKey();
            if (!attributeKeys.contains(key)) {
                childEntries.add(new NamedNode(key, entry.getValue()));
            }
        }

        if (childEntries.isEmpty()) {
            if (value != null) {
                parent.setTextContent(value);
            }
            else if (!ignoreValue) {
                parent.setAttribute(KEYWORD_HAS_VALUE, STR_NO);
            }
            return;
        }

        if (value != null) {
            parent.setAttribute(KEYWORD_VALUE, value);
        }

        Collections.sort(childEntries, (o1, o2) -> nodeProperties.compare(o1.name, o2.name));

        for (NamedNode child: childEntries) {
            String xmlKey = toElementName(child.name);
            ConfigNodeProperty childSorter = nodeProperties.getChildSorter(child.name);

            for (ConfigTree childTree: child.trees) {
                Element childElement = document.createElement(xmlKey);
                parent.appendChild(childElement);

                ConfigTree adjustedChildTree = childSorter.adjustNodes(childTree);
                addTreeToXml(document, childElement, adjustedChildTree, childSorter);
            }
        }
    }

    public static void addTree(Element parent, ConfigTree tree, ConfigNodeProperty nodeProperties) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(nodeProperties, "nodeProperties");

        Document document = parent.getOwnerDocument();
        Objects.requireNonNull(document, "parent.getOwnerDocument()");
        addTreeToXml(document, parent, tree, nodeProperties);
    }

    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    public static Document createXml(ConfigTree tree) throws ParserConfigurationException {
        Document result = newDocumentBuilder().newDocument();
        Element root = result.createElement("gradle-project-properties");
        result.appendChild(root);

        String comment = "DO NOT EDIT THIS FILE! - Used by the Gradle plugin of NetBeans.";
        root.appendChild(result.createComment(comment));

        addTree(root, tree, CompatibleRootNodeProperty.INSTANCE);

        return result;
    }

    private static int nullSafeStrCmp(String str1, String str2) {
        if (str1 == null) {
            return str2 != null ? -1 : 0;
        }
        else if (str2 == null) {
            return 1;
        }

        return str1.compareTo(str2);
    }

    public static void addAuxiliary(Document document, Element... auxElements) {
        Element root = Objects.requireNonNull(document.getDocumentElement(),
                "document.getDocumentElement()");

        if (auxElements.length == 0) {
            return;
        }

        Element[] sortedAuxElements = auxElements.clone();
        Arrays.sort(sortedAuxElements, (Element o1, Element o2) -> {
            String uri1 = o1.getNamespaceURI();
            String uri2 = o2.getNamespaceURI();
            int uriCmp = nullSafeStrCmp(uri1, uri2);
            if (uriCmp != 0) {
                return uriCmp;
            }

            return nullSafeStrCmp(o1.getNodeName(), o2.getNodeName());
        });

        Element auxRoot = document.createElement(AUXILIARY_NODE_NAME);
        root.appendChild(auxRoot);

        for (Element auxElement: sortedAuxElements) {
            auxRoot.appendChild(document.importNode(auxElement, true));
        }
    }

    public static void savePrettyXmlDocument(Document document, Result result) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(result, "result");

        Source source = new DOMSource(document);

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, XML_ENCODING);
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            try {
                // If not set, some JDKs might not emit a new line after the XML header.
                transformer.setOutputProperty("http://www.oracle.com/xml/is-standalone", "yes");
            } catch (IllegalArgumentException e) {
                // Expected to be thrown by JDKs unaffected of this issu
            }

            transformer.transform(source, result);
        } catch (TransformerException ex) {
            throw new IOException(ex);
        }
    }

    public static ConfigSaveOptions getSaveOptions(Project project, Path output) {
        String lineSeparator = NbFileUtils.tryGetLineSeparatorForTextFile(output);
        if (lineSeparator == null) {
            lineSeparator = ChangeLFPlugin.getPreferredLineSeparator(project);
        }
        return new ConfigSaveOptions(lineSeparator);
    }

    public static void saveXmlTo(Document document, Path output, ConfigSaveOptions saveOptions) throws IOException {
        Objects.requireNonNull(saveOptions, "saveOptions");

        saveXmlTo(document, output, saveOptions.getPreferredLineSeparator());
    }

    public static void saveXmlTo(
            Document document,
            Path output,
            String lineSeparator) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(output, "output");

        if (lineSeparator == null) {
            Result result = new StreamResult(output.toFile());
            savePrettyXmlDocument(document, result);
        }
        else {
            StringWriter writer = new StringWriter(FILE_BUFFER_SIZE);
            Result result = new StreamResult(writer);
            savePrettyXmlDocument(document, result);

            String fileOutput = writer.toString();
            BufferedReader configContent = new BufferedReader(new StringReader(fileOutput));

            StringBuilder newFileStrContent = new StringBuilder(fileOutput.length());
            for (String line = configContent.readLine(); line != null; line = configContent.readLine()) {
                newFileStrContent.append(line);
                newFileStrContent.append(lineSeparator);
            }

            Files.write(output, newFileStrContent.toString().getBytes(XML_ENCODING));
        }
    }

    private static final class KeyValuePair {
        public final String key;
        public final String value;

        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class NamedNode {
        public final String name;
        public final List<ConfigTree> trees;

        public NamedNode(String name, List<ConfigTree> trees) {
            this.name = name;
            this.trees = trees;
        }
    }

    private ConfigXmlUtils() {
        throw new AssertionError();
    }
}
