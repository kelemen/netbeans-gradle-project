package org.netbeans.gradle.project.util;

import org.jtrim2.utils.ExceptionHelper;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.netbeans.junit.MockServices;

public final class MockServicesRule implements TestRule {
    private final Class<?>[] services;

    public MockServicesRule(Class<?>... services) {
        this.services = services.clone();
        ExceptionHelper.checkNotNullElements(this.services, "services");
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                MockServices.setServices(services);
                try {
                    base.evaluate();
                } finally {
                    MockServices.setServices();
                }
            }
        };
    }
}
