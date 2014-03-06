package org.netbeans.gradle.project.properties2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.w3c.dom.Element;

public interface PropertyXmlDef<ValueKey> {
    @Nullable
    public ValueKey loadFromXml(@Nonnull Element node);
    public void addToXml(@Nonnull Element parent, @Nonnull ValueKey value);
}
