package org.nbandroid.netbeans.gradle;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.modules.java.platform.JavaPlatformProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;

/**
 *
 * @author radim
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.java.platform.JavaPlatformProvider.class)
public class PlatformProvider implements JavaPlatformProvider {
  private static final String GRADLE_JDK = System.getProperty("test.all.gradle.jdk");
  private final JavaPlatform p;
  
  public PlatformProvider() {
    p = new JavaPlatform() {

      @Override
      public String getDisplayName() {
        return "fake platform to run gradle daemon";
      }

      @Override
      public Map<String, String> getProperties() {
        return Collections.EMPTY_MAP;
      }

      @Override
      public ClassPath getBootstrapLibraries() {
        return ClassPathSupport.createClassPath(new FileObject[0]);
      }

      @Override
      public ClassPath getStandardLibraries() {
        return ClassPathSupport.createClassPath(new FileObject[0]);
      }

      @Override
      public String getVendor() {
        return "dummy";
      }

      @Override
      public Specification getSpecification() {
        return new Specification("J2SE", new SpecificationVersion("1.6"));
      }

      @Override
      public Collection<FileObject> getInstallFolders() {
        return Collections.singleton(FileUtil.toFileObject(new File(GRADLE_JDK)));
      }

      @Override
      public FileObject findTool(String toolName) {
        return null;
      }

      @Override
      public ClassPath getSourceFolders() {
        return ClassPathSupport.createClassPath(new FileObject[0]);
      }

      @Override
      public List<URL> getJavadocFolders() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public JavaPlatform[] getInstalledPlatforms() {
    return new JavaPlatform[] { p };
  }

  @Override
  public JavaPlatform getDefaultPlatform() {
    return p;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
  }
  
}
