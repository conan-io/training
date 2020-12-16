package conan.ci.docker

import conan.ci.arg.ArgumentList
import conan.ci.jenkins.JenkinsAgent
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

abstract class DockerClient {
    CpsScript currentBuild
    Map config
    JenkinsAgent jenkinsAgent
    ArgumentList args
    String workdir
    String shell
    String imageName
    String os
    String runCommand

    abstract String readFile(String path)

    abstract void configureGitAuth(DockerCommandRunner dcr)

    String wrapCommandForShell(String commandToRun) {
        if (args.asMap['dockerRunCommandRaw']) {
            return "\'${commandToRun}\'"
        } else {
            return "${shell} '${commandToRun}'"
        }
    }

    void configureGit(DockerCommandRunner dcr) {
        dcr.run('git config --global user.email "conanan@training.ci"')
        dcr.run('git config --global user.name "Conan Training CI"')
        dcr.run('git config --global push.default simple')
        configureGitAuth(dcr)
    }

    void withRun(String stageName, Closure body) {
        String networkName = args.asMap['dockerNetworkName']
        currentBuild.stage(stageName) {
            currentBuild.docker.image(imageName)
                    .withRun("-t ${networkName}", runCommand) { def container ->
                        DockerCommandRunner dcr = DockerCommandRunner
                                .construct(currentBuild, this, (String) container.id)
                        body(dcr)
                    }
        }
    }
}
