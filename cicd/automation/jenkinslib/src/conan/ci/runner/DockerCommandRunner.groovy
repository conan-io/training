package conan.ci.runner

class DockerCommandRunner implements ICommandRunner {
    def currentBuild

    DockerCommandRunner(def currentBuild) {
        this.currentBuild = currentBuild
    }

    @Override
    def run(String commandToRun, Boolean returnStdOut = true) {
        String detectedWorkingDir = new conan.ci.docker.DockerContainerInspector(currentBuild).detectWorkingDirectory()
        conan.ci.arg.DirArgs runArgs = new conan.ci.arg.DirArgs(currentBuild, ["workingDir": detectedWorkingDir])
        run(commandToRun, runArgs.fullWorkingDir, returnStdOut)
    }

    @Override
    def run(String commandToRun, String workingDirectory, Boolean returnStdOut = true) {
        NativeCommandRunner nativeCommandRunner = new NativeCommandRunner(currentBuild)
        String detectedShell = new conan.ci.docker.DockerContainerInspector(currentBuild).detectContainerShell()
        conan.ci.arg.DockerArgs dockerArgs = new conan.ci.arg.DockerArgs(currentBuild)
        String containerUser = dockerArgs.getContainerUser()
        String command = dockerArgs.getExecCommand(commandToRun, workingDirectory, containerUser, detectedShell)
        return nativeCommandRunner.run(command, returnStdOut)
    }
}
