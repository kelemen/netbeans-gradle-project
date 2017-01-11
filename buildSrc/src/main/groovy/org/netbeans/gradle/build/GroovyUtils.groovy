package org.netbeans.gradle.build

import org.gradle.api.Action

class GroovyUtils {
    static Closure toClosure(Provider action) {
        return {
            return action.get()
        }
    }
}

