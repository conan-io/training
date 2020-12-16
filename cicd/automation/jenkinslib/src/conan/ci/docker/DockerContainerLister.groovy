package conan.ci.docker

import conan.ci.arg.archive.DockerBinding
import conan.ci.runner.NativeCommandRunner
import org.jenkinsci.plugins.workflow.cps.CpsScript

class DockerContainerLister {
    CpsScript currentBuild
    DockerContainerLister(CpsScript currentBuild) {
        this.currentBuild = currentBuild
    }

    List<String> list() {
        DockerBinding dockerArgs = new DockerBinding(currentBuild)
        def commandRunner = NativeCommandRunner.construct(currentBuild)
        def command = dockerArgs.getPsCommand()
        String stdout = commandRunner.run(command)
        List<String> containers
        if (stdout){
            containers = stdout.trim().split("\n")
        }else{
            containers = []
        }
        return containers
    }
}
