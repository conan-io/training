package conan.ci.step

import conan.ci.util.DemoLogger

class UtilitySteps {

    static void withMapEnv(def currentBuild, Map<String, String> srcEnv, Closure closure) {
        DemoLogger.debug(currentBuild, "Running withMapEnv")
        withEnv(currentBuild, srcEnv, closure)
    }

    static void withEnv(def currentBuild, Map<String, String> srcEnv, Closure closure) {
        // This entire function is workaround for bug in withEnv in JenkinsPipelineUnit (unit tests)
        // https://github.com/jenkinsci/JenkinsPipelineUnit/issues/240

        Map<String, String> originalEnvVars = [:]
        srcEnv.each{String k, String v ->
            originalEnvVars[k] = currentBuild.env[k] as String
            currentBuild.env[k] = v
        }
        closure()
        originalEnvVars.each{String k, String v ->
            currentBuild.env[k] = v
        }
    }

    static void writeEnvVarsToFile(def currentBuild, Map<String, String> varToFilenameMap) {
        varToFilenameMap.each{String variableName, String fileName ->
            currentBuild.writeFile(file: fileName, text: currentBuild.env[variableName])
        }
    }
}