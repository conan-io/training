package conan.ci.step

import conan.ci.step.workflow.ConanPackagePipeline
import conan.ci.step.workflow.ConanProductPipeline
import conan.ci.step.workflow.ConanFromUpstream
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ConanWorkflowSteps {

    static void conanPackagePipeline(CpsScript currentBuild, Map config=null) {
        Map defaultConfig = [
                "conanRemoteUploadName": "conan-temp",
                "dockerEnvVars"        : [
                        "TRAINING_GIT_CREDS_USR",
                        "TRAINING_GIT_CREDS_PSW",
                        "TRAINING_ART_CREDS_USR",
                        "TRAINING_ART_CREDS_PSW",
                        "GIT_BRANCH",
                ],
        ]
        Map mergedConfig = defaultConfig
        if(config) mergedConfig.putAll(config)
        ConanPackagePipeline.construct(currentBuild, mergedConfig).run()
    }

    static void conanProductPipeline(CpsScript currentBuild, Map config=null) {
        Map defaultConfig = [
                "conanRemoteUploadName": "conan-temp",
                "dockerEnvVars"        : [
                        "TRAINING_GIT_CREDS_USR",
                        "TRAINING_GIT_CREDS_PSW",
                        "TRAINING_ART_CREDS_USR",
                        "TRAINING_ART_CREDS_PSW",
                        "GIT_BRANCH",
                ],
        ]
        Map mergedConfig = defaultConfig
        if(config) mergedConfig.putAll(config)
        ConanProductPipeline.construct(currentBuild, mergedConfig).run()
    }

    static void conanFromUpstream(CpsScript currentBuild, Map config=null) {
        Map defaultConfig = [
                "conanRemoteUploadName": "conan-temp",
                "dockerEnvVars"        : [
                        "TRAINING_GIT_CREDS_USR",
                        "TRAINING_GIT_CREDS_PSW",
                        "TRAINING_ART_CREDS_USR",
                        "TRAINING_ART_CREDS_PSW",
                        "GIT_BRANCH",
                ],
        ]
        Map mergedConfig = defaultConfig
        if(config) mergedConfig.putAll(config)
        ConanFromUpstream.construct(currentBuild, mergedConfig).run()
    }

    static void conanPromotionPipeline(CpsScript currentBuild, Map config=null) {
        Map defaultConfig = [
                "conanRemoteUploadName": "conan-develop",
                "dockerEnvVars"        : [
                        "TRAINING_GIT_CREDS_USR",
                        "TRAINING_GIT_CREDS_PSW",
                        "TRAINING_ART_CREDS_USR",
                        "TRAINING_ART_CREDS_PSW",
                        "GIT_BRANCH",
                ],
        ]
        Map mergedConfig = defaultConfig
        if(config) mergedConfig.putAll(config)
        ConanFromUpstream.construct(currentBuild, mergedConfig).run()
    }

}
