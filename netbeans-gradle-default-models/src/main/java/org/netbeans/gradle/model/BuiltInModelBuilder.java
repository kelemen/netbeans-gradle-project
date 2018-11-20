package org.netbeans.gradle.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.BuildController;
import org.netbeans.gradle.model.util.BuilderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class BuiltInModelBuilder implements BuildInfoBuilder<Map<Class<?>, Object>> {
    private static final long serialVersionUID = 1L;

    private final Set<Class<?>> modelClasses;

    public BuiltInModelBuilder(Class<?>... modelClasses) {
        this.modelClasses = new HashSet<Class<?>>(Arrays.asList(modelClasses));

        CollectionUtils.checkNoNullElements(this.modelClasses, "modelClasses");
    }

    @Override
    public Map<Class<?>, Object> getInfo(BuildController controller) {
        Map<Class<?>, Object> result = new IdentityHashMap<Class<?>, Object>();
        for (Class<?> modelClass: modelClasses) {
            Object model = controller.findModel(modelClass);
            if (model != null) {
                result.put(modelClass, model);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return BuilderUtils.getNameForGenericBuilder(this, modelClasses.toString());
    }
}
