package conan.ci.docker


import conan.ci.runner.NativeCommandRunner
class DockerContainerLister {
    def currentBuild
    DockerContainerLister(def currentBuild) {
        this.currentBuild = currentBuild
    }

    List<String> list() {
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        def commandRunner = new NativeCommandRunner(currentBuild)
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
