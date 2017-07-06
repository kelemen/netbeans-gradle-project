package org.netbeans.gradle.build

import java.util.function.Supplier
import org.gradle.api.Action

class GroovyUtils {
    static Closure toClosure(Supplier<?> action) {
        return {
            return action.get()
        }
    }
}

