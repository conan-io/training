package conan.ci.step

import com.cloudbees.groovy.cps.NonCPS
import conan.ci.constant.DemoConstant
import conan.ci.jenkins.CauseWrapper
import hudson.model.Cause
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

class CopyArtifactSteps {

    @NonCPS
    static void getLockfilesFromUpstream(def currentBuild) {
        // It's not good, but we get have to validate that it's an upstream cause in a previous step
        // Passing Hudson Causes around makes @NonCPS viral in methods and such
        RunWrapper runWrapper = currentBuild.currentBuild as RunWrapper
        Cause causeType = runWrapper.getRawBuild().getCauses().last()
        Cause.UpstreamCause upstreamCause = causeType as Cause.UpstreamCause
        String upstreamProject = upstreamCause.upstreamProject
        int upstreamBuild = upstreamCause.upstreamBuild
        List<String> antPatternsToCopy = [
                "**/${DemoConstant.DOCKER_IMAGE_FILENAME}",
                "**/${DemoConstant.CONAN_LOCKFILE_FILENAME}",
                "**/${DemoConstant.CONAN_BUILDORDER_FILENAME}",
        ]

        copyArtifact(currentBuild, upstreamProject, upstreamBuild, antPatternsToCopy.join(","))
    }

    static void getCombinedBuildOrderFromUpstream(def currentBuild) {
        // We have to go back to get the first build order file to evaluate our current place in it
        // Otherwise we trigger siblings
        CauseWrapper causeEvaluator = new CauseWrapper(currentBuild)
        String upstreamProject = causeEvaluator.firstUpstreamProject
        int upstreamBuild = causeEvaluator.firstUpstreamBuild
        String copyPattern = "**/${DemoConstant.CONAN_COMBINED_BUILDORDER_FILENAME}"
        copyArtifact(currentBuild, upstreamProject, upstreamBuild, copyPattern)
    }

    static void copyArtifact(def currentBuild, String upstreamProjectName, int upstreamBuild, String antPatterns) {
        currentBuild.copyArtifacts(
                projectName: upstreamProjectName,
                filter: antPatterns,
                target: 'upstream',
                selector: currentBuild.specific("${upstreamBuild}")
        )
    }

    static void saveFilesFromAllBuildsForDownstreams(def currentBuild) {
        List<String> antPatternsToCopy = [
                "**/${DemoConstant.DOCKER_IMAGE_FILENAME}",
                "**/${DemoConstant.CONAN_LOCKFILE_FILENAME}",
                "**/${DemoConstant.CONAN_BUILDORDER_FILENAME}",
        ]
        currentBuild.archiveArtifacts(artifacts : antPatternsToCopy.join(","), allowEmptyArchive : true)
    }

    static void saveCombinedBuildOrderForDownstreams(def currentBuild) {
        currentBuild.archiveArtifacts(artifacts : "**/${DemoConstant.CONAN_COMBINED_BUILDORDER_FILENAME}")
    }
}
