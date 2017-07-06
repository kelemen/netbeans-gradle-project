package org.netbeans.gradle.project.license;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.junit.Test;
import org.netbeans.gradle.project.util.CloseableAction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LicenseManagerTest {
    private static String toModelName(String model) {
        return model + "_NAME";
    }

    private static LicenseHeaderInfo testLicenseInfo(String name, String fileName) {
        return new LicenseHeaderInfo(
                name,
                Collections.<String, String>emptyMap(),
                fileName != null ? Paths.get(fileName) : null);
    }

    private static LicenseManager<String> createTestManager(LicenseStore<DefaultLicenseDef> licenseStore) {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        return LicenseManagers.createLicenseManager(executor, licenseStore, Paths::get, LicenseManagerTest::toModelName);
    }

    @Test
    public void testGetOnEmpty() {
        MemLicenseStore licenseStore = new MemLicenseStore();
        LicenseManager<String> manager = createTestManager(licenseStore);

        LicenseHeaderInfo info = testLicenseInfo("LicenseName", "LicenseFile");

        assertNull(manager.tryGetRegisteredLicenseName("TestModel", info));
        licenseStore.assertEmpty();
    }

    private static void verifyLicenseDef(DefaultLicenseDef licenseDef, String model, String licenseName, String licenseFile) {
        assertEquals("DefaultLicenseDef.getName()", licenseName, licenseDef.getName());
        verifyDisplayName(licenseDef, model, licenseName);
        verifyLicenseSrc(licenseDef, model, licenseFile);
    }

    private static void verifyDisplayName(DefaultLicenseDef licenseDef, String model, String licenseName) {
        String displayName = licenseDef.getDisplayName();
        if (!displayName.contains(licenseName)) {
            fail("Display name must contain the name of the license: " + licenseName + " but was " + displayName);
        }

        String modelName = toModelName(model);
        if (!displayName.contains(modelName)) {
            fail("Display name must contain the name of the model: " + modelName + " but was " + displayName);
        }
    }

    private static void verifyLicenseSrc(DefaultLicenseDef licenseDef, String model, String licenseFile) {
        Path src = licenseDef.getSrc();
        assertEquals("DefaultLicenseDef.getSrc()", Paths.get(model, licenseFile), src);
    }

    @Test
    public void testAddOneGetOne() {
        testAddOneGetOne(false);
    }

    @Test
    public void testAddOneGetOneNestedOpen() {
        testAddOneGetOne(true);
    }

    private void testAddOneGetOne(boolean nestedOpen) {
        MemLicenseStore licenseStore = new MemLicenseStore();
        LicenseManager<String> manager = createTestManager(licenseStore);

        LicenseHeaderInfo info = testLicenseInfo("LicenseName", "LicenseFile");

        MutableProperty<String> modelRef = PropertyFactory.memProperty("TestModel");
        MutableProperty<LicenseHeaderInfo> infoRef = PropertyFactory.memProperty(info);

        PropertySource<CloseableAction> registerActionRef = manager.getRegisterListenerAction(modelRef, infoRef);
        CloseableAction registerAction = registerActionRef.getValue();

        assertNull(manager.tryGetRegisteredLicenseName("TestModel", info));
        licenseStore.assertEmpty();

        try (CloseableAction.Ref closeRef = registerAction.open()) {
            assertNotNull(closeRef);

            if (nestedOpen) {
                registerAction.open().close();
            }

            String licenseId = manager.tryGetRegisteredLicenseName("TestModel", info);
            assertNotNull("licenseId", licenseId);

            DefaultLicenseDef licenseDef = licenseStore.getById(licenseId);

            assertEquals(licenseId, licenseDef.getLicenseId());
            verifyLicenseDef(licenseDef, "TestModel", "LicenseName", "LicenseFile");
        }

        assertNull(manager.tryGetRegisteredLicenseName("TestModel", info));
        licenseStore.assertEmpty();
    }

    @Test
    public void testOverwriteLicenseHeaderInfoAndGet() {
        MemLicenseStore licenseStore = new MemLicenseStore();
        LicenseManager<String> manager = createTestManager(licenseStore);

        LicenseHeaderInfo info1 = testLicenseInfo("LicenseName1", "LicenseFile1");

        MutableProperty<String> modelRef = PropertyFactory.memProperty("TestModel1");
        MutableProperty<LicenseHeaderInfo> infoRef = PropertyFactory.memProperty(info1);

        PropertySource<CloseableAction> registerActionRef = manager.getRegisterListenerAction(modelRef, infoRef);

        Runnable listener = mock(Runnable.class);
        registerActionRef.addChangeListener(listener);

        verifyZeroInteractions(listener);

        LicenseHeaderInfo info2 = testLicenseInfo("LicenseName2", "LicenseFile2");
        infoRef.setValue(info2);

        verify(listener).run();

        CloseableAction registerAction = registerActionRef.getValue();

        assertNull(manager.tryGetRegisteredLicenseName("TestModel1", info2));
        licenseStore.assertEmpty();

        try (CloseableAction.Ref closeRef = registerAction.open()) {
            assertNotNull(closeRef);

            String licenseId = manager.tryGetRegisteredLicenseName("TestModel1", info2);
            assertNotNull("licenseId", licenseId);

            DefaultLicenseDef licenseDef = licenseStore.getById(licenseId);

            assertEquals(licenseId, licenseDef.getLicenseId());
            verifyLicenseDef(licenseDef, "TestModel1", "LicenseName2", "LicenseFile2");
        }

        assertNull(manager.tryGetRegisteredLicenseName("TestModel1", info2));
        licenseStore.assertEmpty();

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testOverwriteModelAndGet() {
        MemLicenseStore licenseStore = new MemLicenseStore();
        LicenseManager<String> manager = createTestManager(licenseStore);

        LicenseHeaderInfo info1 = testLicenseInfo("LicenseName1", "LicenseFile1");

        MutableProperty<String> modelRef = PropertyFactory.memProperty("TestModel1");
        MutableProperty<LicenseHeaderInfo> infoRef = PropertyFactory.memProperty(info1);

        PropertySource<CloseableAction> registerActionRef = manager.getRegisterListenerAction(modelRef, infoRef);

        Runnable listener = mock(Runnable.class);
        registerActionRef.addChangeListener(listener);

        verifyZeroInteractions(listener);

        modelRef.setValue("TestModel2");

        verify(listener).run();

        CloseableAction registerAction = registerActionRef.getValue();

        assertNull(manager.tryGetRegisteredLicenseName("TestModel2", info1));
        licenseStore.assertEmpty();

        try (CloseableAction.Ref closeRef = registerAction.open()) {
            assertNotNull(closeRef);

            String licenseId = manager.tryGetRegisteredLicenseName("TestModel2", info1);
            assertNotNull("licenseId", licenseId);

            DefaultLicenseDef licenseDef = licenseStore.getById(licenseId);

            assertEquals(licenseId, licenseDef.getLicenseId());
            verifyLicenseDef(licenseDef, "TestModel2", "LicenseName1", "LicenseFile1");
        }

        assertNull(manager.tryGetRegisteredLicenseName("TestModel2", info1));
        licenseStore.assertEmpty();

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testWithoutLicenseFile() {
        MemLicenseStore licenseStore = new MemLicenseStore();
        LicenseManager<String> manager = createTestManager(licenseStore);

        LicenseHeaderInfo info = testLicenseInfo("LicenseName", null);

        MutableProperty<String> modelRef = PropertyFactory.memProperty("TestModel");
        MutableProperty<LicenseHeaderInfo> infoRef = PropertyFactory.memProperty(info);

        PropertySource<CloseableAction> registerActionRef = manager.getRegisterListenerAction(modelRef, infoRef);
        CloseableAction registerAction = registerActionRef.getValue();

        assertNull(manager.tryGetRegisteredLicenseName("TestModel", info));
        licenseStore.assertEmpty();

        try (CloseableAction.Ref closeRef = registerAction.open()) {
            assertNotNull(closeRef);

            licenseStore.assertEmpty();
            licenseStore.addToStore(info);

            String licenseId = manager.tryGetRegisteredLicenseName("TestModel", info);
            assertEquals(info.getLicenseName(), licenseId);
        }
    }

    @Test
    public void testWithoutLicenseFileAndWithoutRegistering() {
        MemLicenseStore licenseStore = new MemLicenseStore();
        LicenseManager<String> manager = createTestManager(licenseStore);

        LicenseHeaderInfo info = testLicenseInfo("LicenseName", null);
        licenseStore.addToStore(info);

        String licenseId = manager.tryGetRegisteredLicenseName("TestModel", info);
        assertEquals(info.getLicenseName(), licenseId);
    }

    @Test
    public void testWithoutLicenseFileAndWithoutRegisteringNotInStore() {
        MemLicenseStore licenseStore = new MemLicenseStore();
        LicenseManager<String> manager = createTestManager(licenseStore);

        LicenseHeaderInfo info = testLicenseInfo("LicenseName", null);
        assertNull(manager.tryGetRegisteredLicenseName("TestModel", info));
    }

    private static final class MemLicenseStore implements LicenseStore<DefaultLicenseDef> {
        private final Map<String, DefaultLicenseDef> licenses;

        public MemLicenseStore() {
            this.licenses = new ConcurrentHashMap<>();
        }

        public void addToStore(LicenseHeaderInfo info) {
            String licenseName = info.getLicenseName();
            licenses.put(licenseName, new DefaultLicenseDef(
                    Paths.get("root", licenseName),
                    licenseName,
                    licenseName + "_DISPLAY"));
        }

        public void assertEmpty() {
            if (!licenses.isEmpty()) {
                throw new AssertionError("Expected no licenses but found " + licenses.keySet());
            }
        }

        public DefaultLicenseDef tryGetById(String licenseId) {
            return licenses.get(licenseId);
        }

        public DefaultLicenseDef getById(String licenseId) {
            DefaultLicenseDef result = tryGetById(licenseId);
            if (result == null) {
                throw new AssertionError("Expected to have license with ID: " + licenseId);
            }
            return result;
        }

        @Override
        public void addLicense(DefaultLicenseDef licenseDef) throws IOException {
            licenses.put(licenseDef.getLicenseId(), licenseDef);
        }

        @Override
        public void removeLicense(String licenseId) throws IOException {
            licenses.remove(licenseId);
        }

        @Override
        public boolean containsLicense(String licenseId) {
            return licenses.containsKey(licenseId);
        }
    }
}
