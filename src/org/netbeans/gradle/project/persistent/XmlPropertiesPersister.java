package org.netbeans.gradle.project.persistent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class XmlPropertiesPersister implements PropertiesPersister {
    private static final Logger LOGGER = Logger.getLogger(XmlPropertiesPersister.class.getName());

    private static final String ROOT_NODE = "gradle-project-properties";
    private static final String SOURCE_ENCODING_NODE = "source-encoding";
    private static final String PLATFORM_NODE = "target-platform";
    private static final String SOURCE_LEVEL_NODE = "source-level";

    private final NbGradleProject project;

    public XmlPropertiesPersister(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    private File getPropertyFile() throws IOException {
        NbGradleModel model = project.getCurrentModel();
        FileObject settingsFile = model.getSettingsFile();
        FileObject dir = settingsFile != null
                ? settingsFile.getParent()
                : project.getProjectDirectory();
        if (dir == null) {
            dir = project.getProjectDirectory();
        }

        File outputDir = FileUtil.toFile(dir);
        if (outputDir == null) {
            throw new IOException("Cannot save properties because the directory is missing: " + dir);
        }

        return new File(outputDir, ".nb-gradle-properties");
    }

    private void checkEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method may only be called from the EDT.");
        }
    }

    private static void addSimpleChild(Node parent, String tagName, String value) {
        Element element = parent.getOwnerDocument().createElement(tagName);
        element.setTextContent(value);
        parent.appendChild(element);
    }

    private static String tryGetValueOfNode(Node node, String tagName) {
        NodeList childNodes = node.getChildNodes();
        int childCount = childNodes.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = childNodes.item(i);
            if (tagName.equals(child.getNodeName())) {
                String result = child.getTextContent();
                return result != null ? result.trim() : null;
            }
        }
        return null;
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

    private void saveDocument(Document document) throws TransformerException, IOException {
        Source source = new DOMSource(document);
        Result result = new StreamResult(getPropertyFile());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(source, result);
    }

    @Override
    public void save(ProjectProperties properties) {
        checkEDT();

        final Charset sourceEncoding = properties.getSourceEncoding().getValue();
        final JavaPlatform platform = properties.getPlatform().getValue();
        final String sourceLevel = properties.getSourceLevel().getValue();

        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
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

                if (!sourceEncoding.name().equals(ProjectProperties.DEFAULT_SOURCE_ENCODING.name())) {
                    addSimpleChild(root, SOURCE_ENCODING_NODE, sourceEncoding.name());
                }
                if (!platform.equals(defaultPlatform)) {
                    addSimpleChild(root, PLATFORM_NODE, platform.getSpecification().getVersion().toString());
                }
                if (!sourceLevel.equals(ProjectProperties.getSourceLevelFromPlatform(defaultPlatform))) {
                    addSimpleChild(root, SOURCE_LEVEL_NODE, sourceLevel);
                }

                try {
                    saveDocument(document);
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, "Failed to save the properties.", ex);
                } catch (TransformerException ex) {
                    LOGGER.log(Level.INFO, "Failed to save the properties.", ex);
                }
            }
        });
    }

    @Override
    public void load(final ProjectProperties properties) {
        checkEDT();

        // We must listen for changes, so that we do not overwrite properties
        // modified later.
        final ChangeDetector platformChanged = new ChangeDetector();
        final ChangeDetector sourceEncodingChanged = new ChangeDetector();
        final ChangeDetector sourceLevelChanged = new ChangeDetector();

        properties.getPlatform().addChangeListener(platformChanged);
        properties.getSourceEncoding().addChangeListener(sourceEncodingChanged);
        properties.getSourceLevel().addChangeListener(sourceLevelChanged);

        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DocumentBuilder builder;
                    try {
                        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    } catch (ParserConfigurationException ex) {
                        LOGGER.log(Level.SEVERE, "Failed to create XML builder.", ex);
                        return;
                    }

                    Document document;
                    try {
                        File propertyFile = getPropertyFile();
                        if (!propertyFile.exists()) {
                            return;
                        }

                        document = builder.parse(propertyFile);
                    } catch (SAXException ex) {
                        LOGGER.log(Level.INFO, "Failed to parse the property file.", ex);
                        return;
                    } catch (IOException ex) {
                        LOGGER.log(Level.INFO, "Failed to read the property file.", ex);
                        return;
                    }

                    Element root = document.getDocumentElement();

                    final String sourceLevel = tryGetValueOfNode(root, SOURCE_LEVEL_NODE);

                    String sourceEncodingStr = tryGetValueOfNode(root, SOURCE_ENCODING_NODE);
                    final Charset sourceEncoding = sourceEncodingStr != null
                            ? parseCharset(sourceEncodingStr)
                            : null;

                    String platformStr = tryGetValueOfNode(root, PLATFORM_NODE);
                    final JavaPlatform platform = platformStr != null
                            ? parsePlatform(platformStr)
                            : null;

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (sourceLevel != null && !sourceLevelChanged.hasChanged()) {
                                properties.getSourceLevel().setValue(sourceLevel);
                            }
                            if (platform != null && !platformChanged.hasChanged()) {
                                properties.getPlatform().setValue(platform);
                            }
                            if (sourceEncoding != null && !sourceEncodingChanged.hasChanged()) {
                                properties.getSourceEncoding().setValue(sourceEncoding);
                            }
                        }
                    });

                } finally {
                    // invokeLater is required, so that the listeners will not
                    // be removed before setting the properties.
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            properties.getPlatform().removeChangeListener(platformChanged);
                            properties.getSourceEncoding().removeChangeListener(sourceEncodingChanged);
                            properties.getSourceLevel().removeChangeListener(sourceLevelChanged);
                        }
                    });
                }

            }
        });
    }

    private static class ChangeDetector implements ChangeListener {
        private volatile boolean changed;

        public boolean hasChanged() {
            return changed;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            changed = true;
        }
    }
}
