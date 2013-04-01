package org.netbeans.gradle.project.query;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.DefaultProjectProperties;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.CreateFromTemplateAttributesProvider;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;

public final class GradleTemplateAttrProvider implements CreateFromTemplateAttributesProvider {
    private final NbGradleProject project;

    public GradleTemplateAttrProvider(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        this.project = project;
    }

    private void invokeOnEdtAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause;
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                }

                throw new RuntimeException(cause.getMessage(), cause);
            }
        }
    }

    @Override
    public Map<String, ?> attributesFor(DataObject template, DataFolder target, String name) {
        Map<String,Object> values = new TreeMap<String,Object>();

        final AtomicReference<LicenseHeaderInfo> licenseHeaderRef = new AtomicReference<LicenseHeaderInfo>(null);
        invokeOnEdtAndWait(new Runnable() {
            @Override
            public void run() {
                licenseHeaderRef.set(project.getProperties().getLicenseHeader().getValue());
            }
        });

        LicenseHeaderInfo licenseHeader = licenseHeaderRef.get();

        if (licenseHeader != null) {
            String licenseName = licenseHeader.getLicenseName();
            String fileName = "license-" + licenseName + ".txt";

            if (FileUtil.getConfigFile("Templates/Licenses/" + fileName) != null) {
                values.put("license", licenseName);
                for (Map.Entry<String, String> property: licenseHeader.getProperties().entrySet()) {
                    values.put(property.getKey(), property.getValue());
                }
            }
        }

        FileEncodingQueryImplementation enc = project.getLookup().lookup(FileEncodingQueryImplementation.class);
        Charset encoding = enc.getEncoding(target.getPrimaryFile());
        if (encoding == null) {
            encoding = DefaultProjectProperties.DEFAULT_SOURCE_ENCODING;
        }

        values.put("encoding", encoding.name());
        values.put("name", project.getName());
        values.put("displayName", project.getDisplayName());

        if (values.size() > 0) {
            return Collections.singletonMap("project", values);
        }
        else {
            return null;
        }
    }
}
