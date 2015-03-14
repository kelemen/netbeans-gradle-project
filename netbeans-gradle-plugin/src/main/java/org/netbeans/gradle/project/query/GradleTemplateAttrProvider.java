package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties2.standard.SourceEncodingProperty;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.CreateFromTemplateAttributesProvider;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;

public final class GradleTemplateAttrProvider implements CreateFromTemplateAttributesProvider {
    private final NbGradleProject project;

    public GradleTemplateAttrProvider(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.project = project;
    }

    @Override
    public Map<String, ?> attributesFor(DataObject template, DataFolder target, String name) {
        Map<String, Object> values = new TreeMap<>();

        LicenseHeaderInfo licenseHeader = project.getCommonProperties().licenseHeaderInfo().getActiveValue();
        if (licenseHeader != null) {
            String licenseName = licenseHeader.getPrivateLicenseName(project);
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
            encoding = SourceEncodingProperty.DEFAULT_SOURCE_ENCODING;
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
