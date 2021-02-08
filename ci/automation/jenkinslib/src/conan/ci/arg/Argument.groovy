package conan.ci.arg

import org.jenkinsci.plugins.workflow.cps.CpsScript

class Argument<T> {
    def currentBuild
    String name
    String group
    String envVarName
    String description
    Closure<T> deserializer
    Closure<T> serializer
    Closure<T> deductor
    T reconciledValue
    T input
    String envVarValue
    Boolean required
    Boolean reconciled

    T reconcile(T input = null, CpsScript currentBuild = null) {
        if (!reconciled) {
            reconciled = true
            this.input = input
            if(currentBuild){
                this.envVarValue = currentBuild?.env[envVarName]
            }
            if (this.input) {
                reconciledValue = this.input
            } else if (this.envVarValue) {
                if (deserializer) {
                    reconciledValue = deserializer(this.envVarValue)
                } else {
                    reconciledValue = this.envVarValue as T
                }
            } else if (deductor) {
                reconciledValue = deductor.call()
            }
            if (required) {
                if (!reconciledValue) {
                    if (deductor) {
                        throw new Exception("The provided deduction closure for ${name} did not return a value.")
                    } else {
                        throw new Exception("Either the configuration item ${name} or env var ${envVarName} must be set.")
                    }
                }
            }
        }
        return reconciledValue
    }

    String toString(T input = null, CpsScript currentBuild = null) {
        reconcile(input, currentBuild)
        if (serializer) {
            return serializer?.call(reconciledValue)
        } else {
            return reconciledValue.toString()
        }

    }
}
