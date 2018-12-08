package org.netbeans.gradle.project.coverage;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JacocoModel;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NbCodeCoverage;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.java.tasks.GradleJavaBuiltInCommands;
import org.netbeans.modules.gsf.codecoverage.api.CoverageManager;
import org.netbeans.modules.gsf.codecoverage.api.CoverageProvider;
import org.netbeans.modules.gsf.codecoverage.api.CoverageType;
import org.netbeans.modules.gsf.codecoverage.api.FileCoverageDetails;
import org.netbeans.modules.gsf.codecoverage.api.FileCoverageSummary;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Pair;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation of CoverageProvider for Gradle project infrastructure.
 * Actual implementation suppors jacoco (cobertura TBD)
 * @author sven
 */
public class GradleCoverageProvider implements CoverageProvider {
    private static final Logger LOG = Logger.getLogger(GradleCoverageProvider.class.getName());
    private static final Map<String, String> JACOCO_DTD_BY_ID = ImmutableMap.of(
            "-//JACOCO//DTD Report 1.0//EN", "jacoco-1.0.dtd",
            "-//JACOCO//DTD Report 1.1//EN", "jacoco-1.1.dtd");

    private final JavaExtension javaExt;
    private final Project p;
    private Map<String, GradleSummary> summaryCache;
    private FileChangeListener listener;

    public GradleCoverageProvider(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.p = javaExt.getProject();
        this.summaryCache = null;
        this.listener = null;
    }

    @Override
    public boolean supportsHitCounts() {
        return true;
    }

    @Override
    public boolean supportsAggregation() {
        return false;
    }

    private NbCodeCoverage getModel() {
        return javaExt.getCurrentModel().getMainModule().getCodeCoverage();
    }

    private boolean hasPlugin() {
        return getModel().hasCodeCoverage();
    }

    @Override
    public boolean isEnabled() {
        return hasPlugin();
    }

    @Override
    public boolean isAggregating() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAggregating(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getMimeTypes() {
        return Collections.singleton("text/x-java");
    }

    @Override
    public void setEnabled(boolean bln) {
        //TODO: add jacoco to build.gradle
    }

    private @CheckForNull File report() {
        JacocoModel jacocoModel = getModel().tryGetJacocoModel();
        if (jacocoModel == null) {
            return null;
        }

        File result = jacocoModel.getReport().getXml();
        return FileUtil.normalizeFile(result);
    }

    public @Override synchronized void clear() {
        File r = report();
        if (r != null && r.isFile() && r.delete()) {
            summaryCache = null;
            CoverageManager.INSTANCE.resultsUpdated(p, GradleCoverageProvider.this);
        }
    }

    @Override
    public FileCoverageDetails getDetails(FileObject fo, Document doc) {
        String path = srcPath().getResourceName(fo);
        if (path == null) {
            return null;
        }
        GradleDetails det = null;
        synchronized (this) {
            GradleSummary summ = summaryCache != null ? summaryCache.get(path) : null;
            if (summ != null) {
                det = summ.getDetails();
                //we have to set the linecount here, as the entire line span is not apparent from the parsed xml, giving strange results then.
                det.lineCount = doc.getDefaultRootElement().getElementCount();
            }
        }
        return det;
    }

    private @CheckForNull Pair<File, org.w3c.dom.Document> parse() {
        File r = report();
        if (r == null) {
            LOG.fine("undefined report location");
            return null;
        }
        CoverageManager.INSTANCE.setEnabled(p, true); // XXX otherwise it defaults to disabled?? not clear where to call this
        if (listener == null) {
            listener = new FileChangeAdapter() {
                public @Override void fileChanged(FileEvent fe) {
                    fire();
                }
                public @Override void fileDataCreated(FileEvent fe) {
                    fire();
                }
                public @Override void fileDeleted(FileEvent fe) {
                    fire();
                }
                private void fire() {
                    synchronized (GradleCoverageProvider.this) {
                        summaryCache = null;
                    }
                    CoverageManager.INSTANCE.resultsUpdated(p, GradleCoverageProvider.this);
                }
            };
            FileUtil.addFileChangeListener(listener, r);
        }
        if (!r.isFile()) {
            LOG.log(Level.FINE, "missing {0}", r);
            return null;
        }
        if (r.length() == 0) {
            // When not previously existent, seems to get created first and written later; file event picks it up when empty.
            LOG.log(Level.FINE, "empty {0}", r);
            return null;
        }
        try {
            org.w3c.dom.Document report;
            InputSource inputSource = new InputSource(r.toURI().toString());
            report = XMLUtil.parse(inputSource, true, false, XMLUtil.defaultErrorHandler(), (publicId, systemId) -> {
                String dtdResource;
                if (systemId.equals("http://cobertura.sourceforge.net/xml/coverage-04.dtd")) {
                    dtdResource = "coverage-04.dtd";
                }
                else {
                    dtdResource = JACOCO_DTD_BY_ID.get(publicId);
                }

                return dtdResource != null
                        ? new InputSource(GradleCoverageProvider.class.getResourceAsStream(dtdResource))
                        : null;
            });
            LOG.log(Level.FINE, "parsed {0}", r);
            return Pair.of(r, report);
        } catch (IOException | SAXException ex) {
            LOG.log(Level.INFO, "Could not parse " + r, ex);
            return null;
        }
    }


    private ClassPath srcPath() {
        GradleClassPathProvider gcp = p.getLookup().lookup(GradleClassPathProvider.class);
        assert gcp != null;
        ClassPath cp = gcp.getClassPaths(ClassPath.SOURCE);
        assert cp != null;
        return cp;
    }

    @Override
    public List<FileCoverageSummary> getResults() {
        Pair<File, org.w3c.dom.Document> r = parse();
        if (r == null) {
            return null;
        }
        ClassPath src = srcPath();
        List<FileCoverageSummary> summs = new ArrayList<>();
        Map<String, GradleSummary> summaries = new HashMap<>();
        boolean jacoco = hasPlugin();
        NodeList nl = r.second().getElementsByTagName(jacoco ? "sourcefile" : "class"); // NOI18N
        for (int i = 0; i < nl.getLength(); i++) {
            Element clazz = (Element)nl.item(i);
            String filename;
            List<Element> lines;
            String name;
            if (jacoco) {
                filename = ((Element)clazz.getParentNode()).getAttribute("name") + '/' + clazz.getAttribute("name");
                lines = new ArrayList<>();
                for (Element line: XMLUtil.findSubElements(clazz)) {
                    if (line.getTagName().equals("line")) {
                        lines.add(line);
                    }
                }
                name = filename.replaceFirst("[.]java$", "").replace('/', '.');
            }
            else {
                filename = clazz.getAttribute("filename");
                Element linesE = XMLUtil.findElement(clazz, "lines", null); // NOI18N
                lines = linesE != null ? XMLUtil.findSubElements(linesE) : Collections.<Element>emptyList();
                // XXX nicer to collect together nested classes in same compilation unit
                name = clazz.getAttribute("name").replace('$', '.');
            }
            FileObject java = src.findResource(filename); // NOI18N
            if (java == null) {
                continue;
            }
            final GradleSummary summar = summaryOf(java, name, lines, jacoco, r.first().lastModified());
            summaries.put(filename, summar);
            summs.add(summar);
        }
        synchronized (this) {
            summaryCache = summaries;
        }
        return summs;
    }

    private GradleSummary summaryOf(FileObject java, String name, List<Element> lines, boolean jacoco, long lastUpdated) {
        // Not really the total number of lines in the file at all, but close enough - the ones Cobertura recorded.
        int lineCount = 0;
        int executedLineCount = 0;
        Map<Integer, Integer> detLines = new HashMap<>();
        for (Element line : lines) {
            lineCount++;
            String attr = line.getAttribute(jacoco ? "ci" : "hits");
            String num = line.getAttribute(jacoco ? "nr" : "number");
            detLines.put(Integer.valueOf(num) - 1,Integer.valueOf(attr));
            if (!attr.equals("0")) {
                executedLineCount++;
            }
        }
        GradleDetails det = new GradleDetails(java, lastUpdated, lineCount, detLines);
        GradleSummary s = new GradleSummary(java, name, det, executedLineCount);
        return s;
    }

    @Override
    public String getTestAllAction() {
        return GradleJavaBuiltInCommands.TEST_WITH_COVERAGE;
    }

    private static class GradleSummary extends FileCoverageSummary {
        private final GradleDetails details;

        public GradleSummary(FileObject file, String displayName, GradleDetails details, int executedLineCount) {
            super(file, displayName, details.getLineCount(), executedLineCount, 0, 0);
            this.details = details;
            details.setSummary(this);
        }

        GradleDetails getDetails() {
            return details;
        }

    }

    private static class GradleDetails implements FileCoverageDetails {
        private final FileObject fileObject;
        private final long lastUpdated;
        private FileCoverageSummary summary;
        private final Map<Integer, Integer> lineHitCounts;
        int lineCount;

        public GradleDetails(FileObject fileObject, long lastUpdated, int lineCount, Map<Integer, Integer> lineHitCounts) {
            this.fileObject = fileObject;
            this.lastUpdated = lastUpdated;
            this.lineHitCounts = lineHitCounts;
            this.lineCount = lineCount;
        }


        @Override
        public FileObject getFile() {
            return fileObject;
        }

        @Override
        public int getLineCount() {
            return lineCount;
        }

        @Override
        public boolean hasHitCounts() {
            return true;
        }

        @Override
        public long lastUpdated() {
            return lastUpdated;
        }

        @Override
        public FileCoverageSummary getSummary() {
            return summary;
        }
        public void setSummary(FileCoverageSummary summary) {
            this.summary = summary;
        }

        @Override
        public CoverageType getType(int lineNo) {
            Integer count = lineHitCounts.get(lineNo);
            return count == null ? CoverageType.INFERRED : count == 0 ? CoverageType.NOT_COVERED : CoverageType.COVERED;
        }

        @Override
        public int getHitCount(int lineNo) {
            Integer ret = lineHitCounts.get(lineNo);
            if (ret == null) {
                return 0;
            }
            return ret;
        }

    }


}
