package conan.ci.docker

import conan.ci.arg.ArgumentList
import conan.ci.arg.DockerArgs
import conan.ci.constant.TrainingConstant
import conan.ci.runner.DockerCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class DockerClientWindows extends DockerClient {
    CpsScript currentBuild

    static DockerClientWindows construct(CpsScript currentBuild, Map config, String imageName) {
        def it = new DockerClientWindows()
        it.currentBuild = currentBuild
        it.config = config
        it.args = ArgumentList.construct(currentBuild, DockerArgs.get())
        it.args.parseArgs(config)
        it.workdir = "C:\\\\Users\\\\ContainerAdministrator"
        it.shell = "cmd -c"
        it.runCommand = "${it.shell} \"cmd.exe\""
        it.imageName = imageName
        it.os = "windows"
        return it
    }

    @Override
    String readFileCommand(String path){
        return "type ${path}"
    }

    @Override
    void configureGitAuthCommand(DockerCommandRunner dcr){
        dcr.run('git config --global unset credential.helper')
    }
}
