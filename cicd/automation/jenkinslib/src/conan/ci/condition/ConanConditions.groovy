package conan.ci.condition


import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanConditions {

    static boolean conanHasSourceChanged(CpsScript currentBuild, Map config = null) {
        Map defaultConfig = [
                "conanRemoteUploadName": "conan-temp",
                "dockerEnvVars"        : [
                        "TRAINING_GIT_CREDS_USR",
                        "TRAINING_GIT_CREDS_PSW",
                        "TRAINING_ART_CREDS_USR",
                        "TRAINING_ART_CREDS_PSW",
                        "TRAINING_CONAN_USER",
                        "TRAINING_CONAN_CHANNEL",
                        "GIT_BRANCH",
                ],
        ]
        Map mergedConfig = defaultConfig
        if (config) mergedConfig.putAll(config)
        return ConanHasSourceChanged.construct(currentBuild, mergedConfig).run()
    }

}
