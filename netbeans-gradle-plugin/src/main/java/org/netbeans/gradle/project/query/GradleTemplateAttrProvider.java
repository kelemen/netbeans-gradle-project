package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.templates.CreateDescriptor;
import org.netbeans.api.templates.CreateFromTemplateAttributes;
import org.netbeans.gradle.project.LicenseManager;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties.standard.SourceEncodingProperty;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;

public final class GradleTemplateAttrProvider implements CreateFromTemplateAttributes {
    private final NbGradleProject project;

    public GradleTemplateAttrProvider(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.project = project;
    }

    @Override
    public Map<String, ?> attributesFor(CreateDescriptor desc) {
        Map<String, Object> values = new TreeMap<>();

        LicenseHeaderInfo licenseHeader = project.getCommonProperties().licenseHeaderInfo().getActiveValue();
        if (licenseHeader != null) {
            String licenseName = LicenseManager.getDefault().tryGetRegisteredLicenseName(project, licenseHeader);
            if (licenseName != null) {
                values.put("license", licenseName);
                for (Map.Entry<String, String> property: licenseHeader.getProperties().entrySet()) {
                    values.put(property.getKey(), property.getValue());
                }
            }
        }

        FileEncodingQueryImplementation enc = project.getLookup().lookup(FileEncodingQueryImplementation.class);
        Charset encoding = enc.getEncoding(desc.getTarget());
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
