package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties2.ProfileKey;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class LazyAuxProperties {
    private final NbGradleProject project;
    private final ProfileKey key;
    private final AtomicReference<ProjectProfileSettings> settingsRef;

    private final Lock tmpStoreLock;
    private Map<DomElementKey, Element> tmpStore;
    private final Document importer;

    public LazyAuxProperties(NbGradleProject project, ProfileKey profileKey) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.project = project;
        this.key = profileKey;
        this.tmpStoreLock = new ReentrantLock();
        this.tmpStore = new HashMap<>();
        this.settingsRef = new AtomicReference<>(null);

        try {
            this.importer = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .newDocument();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private ProjectProfileSettings getSettings() {
        ProjectProfileSettings result = settingsRef.get();
        if (result == null) {
            result = project.getPropertiesForProfile(key);
            if (settingsRef.compareAndSet(null, result)) {
                result.notifyWhenLoaded(new Runnable() {
                    @Override
                    public void run() {
                        onSettingsLoaded();
                    }
                });
            }
            else {
                result = settingsRef.get();
            }
        }
        return result;
    }

    private void onSettingsLoaded() {
        ProjectProfileSettings settings = settingsRef.get();
        assert settings != null;

        while (true) {
            List<Map.Entry<DomElementKey, Element>> toStore;
            tmpStoreLock.lock();
            try {
                if (tmpStore == null) {
                    return;
                }

                if (tmpStore.isEmpty()) {
                    tmpStore = null;
                    return;
                }

                toStore = new ArrayList<>(tmpStore.entrySet());
                tmpStore.clear();
            } finally {
                tmpStoreLock.unlock();
            }

            for (Map.Entry<DomElementKey, Element> entry: toStore) {
                settings.setAuxConfigValue(entry.getKey(), entry.getValue());
            }
        }
    }

    public boolean setAuxConfigValue(DomElementKey key, Element value) {
        ProjectProfileSettings settings = getSettings();

        tmpStoreLock.lock();
        try {
            if (tmpStore != null) {
                Element toAdd = value != null ? (Element)importer.importNode(value, true) : null;
                Element prevValue = tmpStore.put(key, toAdd);
                return prevValue != toAdd;
            }
        } finally {
            tmpStoreLock.unlock();
        }

        return settings.setAuxConfigValue(key, value);
    }

    public Element getAuxConfigValue(DomElementKey key) {
        Element result;
        tmpStoreLock.lock();
        try {
            result = tmpStore != null ? tmpStore.get(key) : null;
        } finally {
            tmpStoreLock.unlock();
        }

        if (result != null) {
            return result;
        }

        ProjectProfileSettings settings = getSettings();
        settings.ensureLoadedAndWait();
        return settings.getAuxConfigValue(key);
    }

    public Collection<DomElementKey> getAuxConfigKeys() {
        return getSettings().getAuxConfigKeys();
    }
}
