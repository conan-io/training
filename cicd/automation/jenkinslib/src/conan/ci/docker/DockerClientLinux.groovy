package conan.ci.docker

import conan.ci.arg.ArgumentList
import conan.ci.arg.DockerArgs
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class DockerClientLinux extends DockerClient {
    CpsScript currentBuild

    static DockerClientLinux construct(CpsScript currentBuild, Map config, String imageName) {
        def it = new DockerClientLinux()
        it.currentBuild = currentBuild
        it.config = config
        it.args = ArgumentList.construct(currentBuild, DockerArgs.get())
        it.args.parseArgs(config)
        it.workdir =  "/home/conan"
        it.shell = "bash -c"
        it.runCommand = "${it.shell} 'cat'"
        it.imageName = imageName
        it.os = "linux"
        return it
    }

    @Override
    String readFile(String path) {
        return "cat ${path}"
    }

    @Override
    void configureGitAuth(DockerCommandRunner dcr) {
        dcr.run('git config --global credential.helper ' +
                '"!f() { ' +
                'echo username=\\$TRAINING_GIT_CREDS_USR; ' +
                'echo password=\\$TRAINING_GIT_CREDS_PSW; ' +
                '}; f"')
    }
}
