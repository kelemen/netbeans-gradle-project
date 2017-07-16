package org.netbeans.gradle.project.license;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.executor.MonitorableTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.util.CloseableAction;

public final class LicenseManager<T> {
    private static final Logger LOGGER = Logger.getLogger(LicenseManager.class.getName());

    private final Impl<T, ?, ?> impl;

    public <LK, LD extends LicenseDef> LicenseManager(
            TaskExecutor executor,
            LicenseStore<LD> licenseStore,
            BiFunction<? super T, ? super LicenseHeaderInfo, ? extends LK> licenseKeyFactory,
            BiFunction<? super T, ? super LK, ? extends LD> licenseDefFactory) {
        this.impl = new Impl<>(executor, licenseStore, licenseKeyFactory, licenseDefFactory);
    }

    public String tryGetRegisteredLicenseName(T ownerModel, LicenseHeaderInfo headerInfo) {
        return impl.tryGetRegisteredLicenseName(ownerModel, headerInfo);
    }

    public PropertySource<CloseableAction> getRegisterListenerAction(
            PropertySource<? extends T> modelProperty,
            PropertySource<? extends LicenseHeaderInfo> headerProperty) {
        return PropertyFactory.combine(modelProperty, headerProperty, this::getRegisterListenerAction);
    }

    private CloseableAction getRegisterListenerAction(
            T ownerModel,
            LicenseHeaderInfo header) {
        Objects.requireNonNull(ownerModel, "ownerModel");
        return () -> impl.registerLicense(ownerModel, header);
    }

    private static final class Impl<T, LK, LD extends LicenseDef> {
        private final MonitorableTaskExecutor syncExecutor;

        private final LicenseStore<LD> licenseStore;
        private final BiFunction<? super T, ? super LicenseHeaderInfo, ? extends LK> licenseKeyFactory;
        private final BiFunction<? super T, ? super LK, ? extends LD> licenseDefFactory;

        private final Map<LK, RegisteredLicense<LD>> licenseRegistartions;

        public Impl(
                TaskExecutor executor,
                LicenseStore<LD> licenseStore,
                BiFunction<? super T, ? super LicenseHeaderInfo, ? extends LK> licenseKeyFactory,
                BiFunction<? super T, ? super LK, ? extends LD> licenseDefFactory) {
            this.syncExecutor = TaskExecutors.inOrderExecutor(executor);
            this.licenseStore = Objects.requireNonNull(licenseStore, "licenseStore");
            this.licenseKeyFactory = Objects.requireNonNull(licenseKeyFactory, "licenseKeyFactory");
            this.licenseDefFactory = Objects.requireNonNull(licenseDefFactory, "licenseDefFactory");
            this.licenseRegistartions = new HashMap<>();
        }

        public String tryGetRegisteredLicenseName(T ownerModel, LicenseHeaderInfo headerInfo) {
            Objects.requireNonNull(ownerModel, "ownerModel");
            Objects.requireNonNull(headerInfo, "headerInfo");

            LK key = tryGetLicenseKey(ownerModel, headerInfo);
            RegisteredLicense<LD> registration = key != null
                    ? licenseRegistartions.get(key)
                    : null;

            String licenseId = registration != null
                    ? registration.getLicenseId()
                    : headerInfo.getLicenseName();

            return licenseStore.containsLicense(licenseId) ? licenseId : null;
        }

        private void removeLicense(RegisteredLicense<LD> registration) throws IOException {
            assert syncExecutor.isExecutingInThis();

            licenseStore.removeLicense(registration.getLicenseId());
        }

        private void addLicense(RegisteredLicense<LD> registration) throws IOException {
            assert syncExecutor.isExecutingInThis();

            licenseStore.addLicense(registration.licenseDef);
        }

        private void doUnregister(final LK key) {
            syncExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                RegisteredLicense<LD> registration = licenseRegistartions.get(key);
                if (registration == null) {
                    LOGGER.log(Level.WARNING, "Too many unregister call to LicenseManager.", new Exception());
                    return;
                }

                if (registration.release()) {
                    licenseRegistartions.remove(key);
                    removeLicense(registration);
                }
            }).exceptionally(AsyncTasks::expectNoError);
        }

        private void doRegister(final T ownerModel, final LK key) {
            syncExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                RegisteredLicense<LD> registration = licenseRegistartions.get(key);
                if (registration == null) {
                    registration = new RegisteredLicense<>(getLicenseDef(ownerModel, key));
                    licenseRegistartions.put(key, registration);
                    addLicense(registration);
                }
                else {
                    registration.use();
                }
            }).exceptionally(AsyncTasks::expectNoError);
        }

        public CloseableAction.Ref registerLicense(T ownerModel, LicenseHeaderInfo header) {
            Objects.requireNonNull(ownerModel, "ownerModel");

            if (header == null) {
                return CloseableAction.CLOSED_REF;
            }

            final LK key = tryGetLicenseKey(ownerModel, header);
            if (key == null) {
                return CloseableAction.CLOSED_REF;
            }

            doRegister(ownerModel, key);

            return new CloseableAction.Ref() {
                private final AtomicBoolean unregistered = new AtomicBoolean(false);

                @Override
                public void close() {
                    if (unregistered.compareAndSet(false, true)) {
                        doUnregister(key);
                    }
                }
            };
        }

        private LK tryGetLicenseKey(T ownerModel, LicenseHeaderInfo headerInfo) {
            return licenseKeyFactory.apply(ownerModel, headerInfo);
        }

        private LD getLicenseDef(T ownerModel, LK key) {
            return licenseDefFactory.apply(ownerModel, key);
        }
    }

    private static final class RegisteredLicense<LD extends LicenseDef> {
        private final LD licenseDef;
        private int useCount;

        public RegisteredLicense(LD licenseDef) {
            this.useCount = 1;
            this.licenseDef = licenseDef;
        }

        public String getLicenseId() {
            return licenseDef.getLicenseId();
        }

        public void use() {
            useCount++;
        }

        public boolean release() {
            useCount--;
            return useCount <= 0;
        }
    }
}
