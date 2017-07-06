package org.netbeans.gradle.project.properties.global;

import java.util.prefs.PreferenceChangeListener;
import org.jtrim2.event.ListenerRef;

public interface BasicPreference {
    public void put(String key, String value);
    public void remove(String key);
    public String get(String key);
    public ListenerRef addPreferenceChangeListener(PreferenceChangeListener pcl);
}
