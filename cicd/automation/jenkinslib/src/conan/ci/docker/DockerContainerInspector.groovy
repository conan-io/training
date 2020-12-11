package conan.ci.docker


import conan.ci.runner.NativeCommandRunner
class DockerContainerInspector {
    def currentBuild

    DockerContainerInspector(def currentBuild) {
        this.currentBuild = currentBuild
    }

    String detectWorkingDirectory() {
        if (containerOs == "linux") {
            return "/home/conan"
        } else {
            return "C:\\Users\\ContainerAdministrator"
        }
    }

    String detectContainerShell() {
        if (containerOs == "linux") {
            return "bash -c"
        } else {
            return "cmd -c"
        }
    }

    String getContainerOs() {
        return containerOsFromEnv() ?: dockerInpsectOs()
    }

    String containerOsFromEnv() {
        return new conan.ci.arg.DockerArgs(currentBuild).getContainerOs()
    }

    String dockerInpsectOs() {
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        def commandRunner = new NativeCommandRunner(currentBuild)
        def command = dockerArgs.getInspectCommand("-f '{{ .Os }}'")
        String detectedOs = commandRunner.run(command).trim()
        return detectedOs
    }
}
