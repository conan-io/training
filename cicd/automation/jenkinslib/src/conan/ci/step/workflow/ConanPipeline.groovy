package conan.ci.step.workflow

import conan.ci.arg.Argument
import conan.ci.arg.ArgumentList
import conan.ci.docker.DockerClientFactory
import conan.ci.jenkins.JenkinsAgent
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

abstract class ConanPipeline {
    CpsScript currentBuild
    JenkinsAgent jenkinsAgent
    DockerClientFactory dockerClientFactory
    Map config
    ArgumentList args

    void printArgs() {
        currentBuild.echo(args.toString())
    }

    List<Argument> pipelineArgs() {
        return [
                new Argument<String>(
                        name: "scriptsUrl",
                        group: "pipeline",
                        envVarName: "TRAINING_SCRIPTS_URL",
                        description: "Url to use to checkout scripts used in CI jobs.",
                ),
                new Argument<String>(
                        name: "lockBranch",
                        group: "pipeline",
                        envVarName: "LOCK_BRANCH",
                        description: "Branch value from job parameter, used when job is triggered by upstream.",
                ),
        ]
    }

    void configureConan(DockerCommandRunner dcr) {
        String conanUsername = args.asMap['artifactoryUser']
        String conanPassword = args.asMap['artifactoryPassword']
        dcr.run("conan config install configs")
        dcr.run("conan user -p ${conanPassword} -r conan-develop ${conanUsername}")
        dcr.run("conan user -p ${conanPassword} -r conan-temp ${conanUsername}")
    }
}
